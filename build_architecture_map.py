#!/usr/bin/env python3
"""Build deterministic hierarchy and dependency maps for Dissolver Enhanced.

The graph models the workspace as:
  Common -> shared files/symbols
  Game -> loader -> version -> files/symbols

Dependency edges are intentionally conservative: Java imports resolve by fully
qualified type name, C# usings resolve by namespace, and unresolved dependencies
become explicit external nodes. The generator never guesses a dependency from a
short symbol name across unrelated worktrees.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from collections import Counter, defaultdict
from pathlib import Path

TEXT_SUFFIXES = {
    ".java", ".cs", ".py", ".gradle", ".properties", ".json", ".toml",
    ".yml", ".yaml", ".xml", ".md", ".sh", ".txt",
}
IGNORED_PARTS = {".git", ".gradle", "build", "out", "bin", "obj", "node_modules", "graphify-out"}

JAVA_PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
JAVA_TYPE_RE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_]\w*)")
JAVA_IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)(?:\.\*)?\s*;", re.MULTILINE)
CS_NAMESPACE_RE = re.compile(r"^\s*namespace\s+([\w.]+)\s*[;{]", re.MULTILINE)
CS_TYPE_RE = re.compile(r"\b(?:class|interface|enum|struct|record)\s+([A-Za-z_]\w*)")
CS_USING_RE = re.compile(r"^\s*using\s+(?:\w+\s*=\s+)?([\w.]+)\s*;", re.MULTILINE)
PY_TYPE_RE = re.compile(r"^(?:async\s+def|def|class)\s+([A-Za-z_]\w*)", re.MULTILINE)
PY_IMPORT_RE = re.compile(r"^\s*(?:from\s+([\w.]+)\s+import|import\s+([\w.]+))", re.MULTILINE)

PALETTE = {
    "workspace": "#f5c76a",
    "common": "#d798ff",
    "game": "#61d6ff",
    "loader": "#73f0b8",
    "version": "#ff9a75",
    "file": "#b5c2db",
    "symbol": "#7c8cff",
    "namespace": "#aa8cff",
    "external": "#f0bd65",
    "ambiguous": "#ff746b",
}
RELATION_STYLE = {
    "CONTAINS": {"color": "#9bb9ff", "dash": ""},
    "DECLARES": {"color": "#7c8cff", "dash": ""},
    "IMPORTS": {"color": "#6cb8ff", "dash": ""},
    "EXTERNAL_IMPORT": {"color": "#f0bd65", "dash": "4,3"},
    "AMBIGUOUS_IMPORT": {"color": "#ff746b", "dash": "2,3"},
    "USES_COMMON": {"color": "#d798ff", "dash": "7,3"},
    "LOADER_EQUIVALENT_EXACT": {"color": "#73f0b8", "dash": "2,2"},
    "LOADER_EQUIVALENT_VARIANT": {"color": "#4cd2d2", "dash": "7,3"},
}


def git_files(path: Path) -> list[Path]:
    result = subprocess.run(["git", "-C", str(path), "ls-files", "-z"], check=True, capture_output=True)
    files: list[Path] = []
    for value in result.stdout.decode("utf-8", "replace").split("\0"):
        if not value:
            continue
        candidate = Path(value)
        if candidate.suffix.lower() not in TEXT_SUFFIXES and candidate.name not in {"Dockerfile", "Makefile"}:
            continue
        if any(part in IGNORED_PARTS for part in candidate.parts):
            continue
        full_path = path / candidate
        if full_path.is_file() and full_path.stat().st_size <= 1_500_000:
            files.append(candidate)
    return sorted(files)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def content_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def node_id(kind: str, *parts: str) -> str:
    return f"{kind}:" + ":".join(parts)


def title_loader(loader: str) -> str:
    return {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge", "quilt": "Quilt", "smapi": "SMAPI"}.get(loader.lower(), loader)


def add_node(nodes: dict[str, dict], ident: str, label: str, kind: str, **extra: object) -> None:
    nodes.setdefault(ident, {"id": ident, "label": label, "kind": kind, "color": PALETTE[kind], **extra})


def add_link(links: dict[tuple[str, str, str], dict], source: str, target: str, relation: str, **extra: object) -> None:
    if source == target:
        return
    links.setdefault((source, target, relation), {"source": source, "target": target, "relation": relation, **extra})


def external_group(value: str) -> str:
    """Collapse unknown imports by useful framework/package prefixes."""
    for prefix in (
        "net.minecraft", "net.fabricmc", "net.minecraftforge", "net.neoforged",
        "org.quiltmc", "StardewModdingAPI", "Microsoft.", "System.", "java.", "javax.",
    ):
        if value.startswith(prefix):
            return prefix.rstrip(".")
    return ".".join(value.split(".")[:2]) if "." in value else value


def source_model(path: Path) -> tuple[list[dict], list[dict], list[str]]:
    """Return declarations, imports/usings, and namespaces for one source file."""
    text = read_text(path)
    suffix = path.suffix.lower()
    declarations: list[dict] = []
    imports: list[dict] = []
    namespaces: list[str] = []
    if suffix == ".java":
        package_match = JAVA_PACKAGE_RE.search(text)
        package = package_match.group(1) if package_match else ""
        for name in JAVA_TYPE_RE.findall(text):
            declarations.append({"name": name, "qualified": f"{package}.{name}" if package else name})
        imports = [{"kind": "java", "value": value} for value in JAVA_IMPORT_RE.findall(text)]
        if package:
            namespaces.append(package)
    elif suffix == ".cs":
        namespace_match = CS_NAMESPACE_RE.search(text)
        namespace = namespace_match.group(1) if namespace_match else ""
        for name in CS_TYPE_RE.findall(text):
            declarations.append({"name": name, "qualified": f"{namespace}.{name}" if namespace else name})
        imports = [{"kind": "csharp", "value": value} for value in CS_USING_RE.findall(text)]
        if namespace:
            namespaces.append(namespace)
    elif suffix == ".py":
        for name in PY_TYPE_RE.findall(text):
            declarations.append({"name": name, "qualified": name})
        imports = [{"kind": "python", "value": left or right} for left, right in PY_IMPORT_RE.findall(text)]
    return declarations, imports, namespaces


def scope_for_branch(relative_root: Path) -> tuple[str, str, str, str]:
    game = relative_root.parts[0]
    leaf = relative_root.name
    loader, version = leaf.split("-", 1) if "-" in leaf else (leaf, "unknown")
    return game, loader, version, f"{title_loader(loader)} {version}"


def relation_js() -> str:
    return json.dumps(RELATION_STYLE, ensure_ascii=False)


def make_2d_html(graph_json: str) -> str:
    return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dissolver Enhanced architecture map (2D)</title><script src="https://cdn.jsdelivr.net/npm/d3@7"></script>
<style>:root{color-scheme:dark}body{margin:0;background:#0b1020;color:#e9efff;font:14px ui-sans-serif,system-ui;overflow:hidden}#toolbar{position:fixed;z-index:2;top:16px;left:16px;width:310px;padding:14px;border:1px solid #2a385d;border-radius:12px;background:#111a31e8;box-shadow:0 12px 35px #0008}h1{font-size:15px;margin:0 0 7px}p{color:#aebddd;line-height:1.35;margin:0 0 11px}input{box-sizing:border-box;width:100%;padding:8px 10px;color:#fff;background:#0b1020;border:1px solid #3b4d78;border-radius:7px}#legend{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}.tag{padding:3px 6px;border-radius:5px;background:#1d2947;font-size:11px}#details{position:fixed;z-index:2;bottom:16px;left:16px;max-width:420px;padding:10px 13px;border-radius:10px;background:#111a31e8;border:1px solid #2a385d;display:none}svg{width:100vw;height:100vh}.node{stroke:#07101f;stroke-width:1.25px;cursor:pointer}.label{fill:#e9efff;pointer-events:none;font-size:10px;text-shadow:0 1px 2px #000}</style></head><body>
<section id="toolbar"><h1>Dissolver Enhanced · Architecture map</h1><p>Exact local imports are solid blue. External imports are gold. Ambiguous imports are red and never point at arbitrary project code. Green/cyan dashed links show cross-loader equivalents.</p><input id="search" placeholder="Find a game, loader, version, file, or symbol…"><div id="legend"></div></section><aside id="details"></aside><svg></svg><script>
const GRAPH=__GRAPH_DATA__, REL=__RELATION_STYLE__;const color=Object.fromEntries(GRAPH.nodes.map(n=>[n.kind,n.color]));const kinds=[...new Set(GRAPH.nodes.map(n=>n.kind))];d3.select('#legend').selectAll('span').data(kinds).join('span').attr('class','tag').style('border-left',d=>`5px solid ${color[d]}`).text(d=>d);
const svg=d3.select('svg'),width=innerWidth,height=innerHeight;const link=svg.append('g').selectAll('line').data(GRAPH.links).join('line').attr('stroke',d=>(REL[d.relation]||{}).color||'#8498c8').attr('stroke-opacity',.42).attr('stroke-dasharray',d=>(REL[d.relation]||{}).dash||'');const node=svg.append('g').selectAll('circle').data(GRAPH.nodes).join('circle').attr('class','node').attr('r',d=>({workspace:16,common:14,game:13,loader:11,version:10,file:4,symbol:3,namespace:5,external:6,ambiguous:6}[d.kind]||4)).attr('fill',d=>d.color);const label=svg.append('g').selectAll('text').data(GRAPH.nodes.filter(d=>!['file','symbol'].includes(d.kind))).join('text').attr('class','label').attr('dx',10).attr('dy',3).text(d=>d.label);
const sim=d3.forceSimulation(GRAPH.nodes).force('link',d3.forceLink(GRAPH.links).id(d=>d.id).distance(d=>d.relation==='CONTAINS'?42:30).strength(d=>d.relation==='CONTAINS'?.9:.25)).force('charge',d3.forceManyBody().strength(d=>d.kind==='file'||d.kind==='symbol'?-15:-140)).force('center',d3.forceCenter(width/2,height/2)).force('collide',d3.forceCollide().radius(d=>({workspace:19,common:17,game:16,loader:14,version:13,file:6,symbol:5,namespace:7,external:8,ambiguous:8}[d.kind]||6)));sim.on('tick',()=>{link.attr('x1',d=>d.source.x).attr('y1',d=>d.source.y).attr('x2',d=>d.target.x).attr('y2',d=>d.target.y);node.attr('cx',d=>d.x).attr('cy',d=>d.y);label.attr('x',d=>d.x).attr('y',d=>d.y)});node.call(d3.drag().on('start',(e,d)=>{if(!e.active)sim.alphaTarget(.25).restart();d.fx=d.x;d.fy=d.y}).on('drag',(e,d)=>{d.fx=e.x;d.fy=e.y}).on('end',(e,d)=>{if(!e.active)sim.alphaTarget(0);d.fx=null;d.fy=null}));const details=d3.select('#details');node.on('click',(e,d)=>{const edges=GRAPH.links.filter(x=>x.source.id===d.id||x.target.id===d.id);details.style('display','block').html(`<b>${d.label}</b><br><small>${d.kind}${d.path?` · ${d.path}`:''}<br>${edges.length} connected edges</small>`)});d3.select('#search').on('input',function(){const q=this.value.trim().toLowerCase();node.attr('opacity',d=>!q||d.label.toLowerCase().includes(q)||d.id.toLowerCase().includes(q)?1:.09);label.attr('opacity',d=>!q||d.label.toLowerCase().includes(q)?1:.09);link.attr('stroke-opacity',q?.08:.42)});
</script></body></html>""".replace("__GRAPH_DATA__", graph_json).replace("__RELATION_STYLE__", relation_js())


def make_3d_html(graph_json: str) -> str:
    return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Dissolver Enhanced architecture map (3D)</title><script src="https://unpkg.com/3d-force-graph"></script><style>:root{color-scheme:dark}body{margin:0;background:#070b16;color:#e9efff;font:14px ui-sans-serif,system-ui}#graph{width:100vw;height:100vh}#panel{position:fixed;z-index:3;top:16px;left:16px;width:310px;padding:14px;border:1px solid #2a385d;border-radius:12px;background:#111a31e8;box-shadow:0 12px 35px #0008}h1{font-size:15px;margin:0 0 7px}p{color:#aebddd;line-height:1.35;margin:0 0 11px}input{box-sizing:border-box;width:100%;padding:8px 10px;color:#fff;background:#0b1020;border:1px solid #3b4d78;border-radius:7px}</style></head><body><section id="panel"><h1>Dissolver Enhanced · Architecture map</h1><p>Green/cyan dashed-style links are cross-loader equivalents. Gold nodes represent external packages, red nodes represent ambiguity.</p><input id="search" placeholder="Highlight a node…"></section><div id="graph"></div><script>
const GRAPH=__GRAPH_DATA__,REL=__RELATION_STYLE__;let query='';const Graph=ForceGraph3D()(document.getElementById('graph')).graphData(GRAPH).backgroundColor('#070b16').nodeLabel(n=>`<b>${n.label}</b><br>${n.kind}${n.path?` · ${n.path}`:''}`).nodeColor(n=>query&&!(`${n.label} ${n.id}`.toLowerCase().includes(query))?'#26324d':n.color).nodeVal(n=>({workspace:18,common:15,game:14,loader:12,version:11,file:3,symbol:2,namespace:4,external:5,ambiguous:5}[n.kind]||3)).linkColor(l=>(REL[l.relation]||{}).color||'rgba(115,150,215,.28)').linkOpacity(.65).linkWidth(l=>l.relation==='CONTAINS'?1.5:(l.relation.includes('EQUIVALENT')?1.2:.55)).linkDirectionalParticles(l=>l.relation==='USES_COMMON'?2:0).linkDirectionalParticleWidth(1.8).onNodeClick(n=>Graph.cameraPosition({x:n.x+80,y:n.y+40,z:n.z+100},n,1200));document.getElementById('search').addEventListener('input',e=>{query=e.target.value.trim().toLowerCase();Graph.nodeColor(n=>query&&!(`${n.label} ${n.id}`.toLowerCase().includes(query))?'#26324d':n.color)});
</script></body></html>""".replace("__GRAPH_DATA__", graph_json).replace("__RELATION_STYLE__", relation_js())


def main() -> int:
    parser = argparse.ArgumentParser(description="Build hierarchy-first 2D and 3D architecture maps.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2], help="Dissolver Enhanced workspace root")
    parser.add_argument("--out", type=Path, default=Path(__file__).resolve().parent, help="Output directory")
    args = parser.parse_args()
    root, output = args.root.resolve(), args.out.resolve()
    output.mkdir(parents=True, exist_ok=True)

    nodes: dict[str, dict] = {}
    links: dict[tuple[str, str, str], dict] = {}
    imports_by_file: dict[str, list[dict]] = {}
    file_scope: dict[str, str] = {}
    symbols_by_fqn: dict[str, list[tuple[str, str]]] = defaultdict(list)
    namespaces_by_fqn: dict[str, list[tuple[str, str]]] = defaultdict(list)
    file_records: list[dict] = []

    workspace_id, common_id = node_id("workspace", "dissolver-enhanced"), node_id("common", "Common")
    add_node(nodes, workspace_id, "Dissolver Enhanced", "workspace")
    add_node(nodes, common_id, "Common", "common", path="Common")
    add_link(links, workspace_id, common_id, "CONTAINS")

    scopes: list[tuple[str, Path, str | None, str | None, str | None]] = [("Common", root / "Common", None, None, None)]
    for game in ("minecraft", "stardewvalley"):
        game_path = root / game
        if not game_path.is_dir():
            continue
        game_id = node_id("game", game)
        add_node(nodes, game_id, "Minecraft" if game == "minecraft" else "Stardew Valley", "game", path=game)
        add_link(links, workspace_id, game_id, "CONTAINS")
        for worktree in sorted(path for path in game_path.iterdir() if path.is_dir() and (path / ".git").exists()):
            game_name, loader, version, label = scope_for_branch(worktree.relative_to(root))
            loader_id, version_id = node_id("loader", game_name, loader), node_id("version", game_name, loader, version)
            add_node(nodes, loader_id, title_loader(loader), "loader", game=game_name)
            add_node(nodes, version_id, label, "version", path=str(worktree.relative_to(root)))
            add_link(links, game_id, loader_id, "CONTAINS")
            add_link(links, loader_id, version_id, "CONTAINS")
            add_link(links, version_id, common_id, "USES_COMMON")
            scopes.append((str(worktree.relative_to(root)), worktree, game_name, loader, version))

    for scope_name, scope_path, game, loader, version in scopes:
        parent_id = common_id if scope_name == "Common" else node_id("version", game or "", loader or "", version or "")
        for relative in git_files(scope_path):
            full_path = scope_path / relative
            file_id = node_id("file", scope_name, relative.as_posix())
            add_node(nodes, file_id, relative.name, "file", path=f"{scope_name}/{relative.as_posix()}", scope=scope_name)
            add_link(links, parent_id, file_id, "CONTAINS")
            file_scope[file_id] = scope_name
            declarations, imported, namespaces = source_model(full_path)
            imports_by_file[file_id] = imported
            for namespace in namespaces:
                namespace_id = node_id("namespace", scope_name, namespace)
                add_node(nodes, namespace_id, namespace, "namespace", scope=scope_name)
                add_link(links, file_id, namespace_id, "DECLARES")
                namespaces_by_fqn[namespace].append((scope_name, namespace_id))
            for symbol in declarations:
                symbol_id = node_id("symbol", scope_name, relative.as_posix(), symbol["qualified"])
                add_node(nodes, symbol_id, symbol["name"], "symbol", path=f"{scope_name}/{relative.as_posix()}", scope=scope_name, qualified=symbol["qualified"])
                add_link(links, file_id, symbol_id, "DECLARES")
                symbols_by_fqn[symbol["qualified"]].append((scope_name, symbol_id))
            file_records.append({"id": file_id, "scope": scope_name, "game": game, "loader": loader, "version": version, "relative": relative.as_posix(), "hash": content_hash(full_path)})

    def resolve_exact(source_id: str, imported: dict) -> None:
        source_scope, value, kind = file_scope[source_id], imported["value"], imported["kind"]
        candidates = namespaces_by_fqn.get(value, []) if kind == "csharp" else symbols_by_fqn.get(value, [])
        same_scope = [ident for scope, ident in candidates if scope == source_scope]
        common_scope = [ident for scope, ident in candidates if scope == "Common"]
        chosen = same_scope or common_scope
        if len(chosen) == 1:
            add_link(links, source_id, chosen[0], "IMPORTS", confidence="exact")
            return
        if len(chosen) > 1:
            ambiguous_id = node_id("ambiguous", source_id, value)
            add_node(nodes, ambiguous_id, f"Ambiguous: {value}", "ambiguous", import_value=value, candidate_count=len(chosen))
            add_link(links, source_id, ambiguous_id, "AMBIGUOUS_IMPORT", confidence="ambiguous")
            return
        external = external_group(value)
        external_id = node_id("external", external)
        add_node(nodes, external_id, external, "external", package=external)
        add_link(links, source_id, external_id, "EXTERNAL_IMPORT", import_value=value, confidence="external")

    for source_id, imported in imports_by_file.items():
        for item in imported:
            resolve_exact(source_id, item)

    # Link same relative files across different loaders. A deterministic star avoids an N² clique.
    by_game_path: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for record in file_records:
        if record["game"] and record["loader"]:
            by_game_path[(record["game"], record["relative"])].append(record)
    for (_, _), records in by_game_path.items():
        loaders = {record["loader"] for record in records}
        if len(records) < 2 or len(loaders) < 2:
            continue
        canonical = sorted(records, key=lambda item: (item["loader"], item["version"], item["scope"]))[0]
        for record in records:
            if record["id"] == canonical["id"] or record["loader"] == canonical["loader"]:
                continue
            relation = "LOADER_EQUIVALENT_EXACT" if record["hash"] == canonical["hash"] else "LOADER_EQUIVALENT_VARIANT"
            add_link(links, record["id"], canonical["id"], relation, relative_path=record["relative"])

    relation_counts = Counter(link["relation"] for link in links.values())
    graph = {
        "schema": "dissolver-enhanced-architecture-v2",
        "description": "Hierarchy-first workspace graph with fully-qualified local imports, explicit external dependencies, ambiguity labels, and cross-loader equivalents.",
        "nodes": sorted(nodes.values(), key=lambda item: item["id"]),
        "links": sorted(links.values(), key=lambda item: (item["source"], item["target"], item["relation"])),
    }
    graph_json = json.dumps(graph, ensure_ascii=False)
    (output / "architecture-graph.json").write_text(json.dumps(graph, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (output / "graph-2d.html").write_text(make_2d_html(graph_json), encoding="utf-8")
    (output / "graph-3d.html").write_text(make_3d_html(graph_json), encoding="utf-8")
    report = [
        "# Dissolver Enhanced Architecture Map", "", "Generated deterministically from tracked text/source files in the local worktrees.", "",
        f"- Nodes: **{len(graph['nodes'])}**", f"- Links: **{len(graph['links'])}**", f"- Files: **{sum(node['kind'] == 'file' for node in graph['nodes'])}**", f"- Symbols: **{sum(node['kind'] == 'symbol' for node in graph['nodes'])}**", "",
        "## Relationship counts", "",
        *[f"- `{relation}`: **{count}**" for relation, count in sorted(relation_counts.items())], "",
        "## Model", "", "- **Common** is the shared hub and contains its tracked files/symbols.", "- **Games** contain loaders, loaders contain version worktrees.", "- `IMPORTS` only represents exact local/Common fully-qualified resolution.", "- `EXTERNAL_IMPORT` represents a collapsed framework/package dependency.", "- `AMBIGUOUS_IMPORT` is explicitly labelled rather than linked to arbitrary code.", "- Cross-loader same-path files are connected by `LOADER_EQUIVALENT_EXACT` or `LOADER_EQUIVALENT_VARIANT`.",
    ]
    (output / "ARCHITECTURE_MAP.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"Built {len(graph['nodes'])} nodes and {len(graph['links'])} links in {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
