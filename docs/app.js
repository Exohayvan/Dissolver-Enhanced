const inputText = document.querySelector("#inputText");
const outputText = document.querySelector("#outputText");
const fileInput = document.querySelector("#fileInput");
const sourceFormat = document.querySelector("#sourceFormat");
const namespaceFallback = document.querySelector("#namespaceFallback");
const sortOutput = document.querySelector("#sortOutput");
const dropZeroes = document.querySelector("#dropZeroes");
const includeHeader = document.querySelector("#includeHeader");
const convertButton = document.querySelector("#convertButton");
const copyButton = document.querySelector("#copyButton");
const downloadButton = document.querySelector("#downloadButton");
const statusEl = document.querySelector("#status");
const summaryEl = document.querySelector("#summary");

const SAMPLE_INPUT = `emc_on_hud=false
private_emc=false
creative_items=false
difficulty=hard
mode=default
emc:minecraft:diamond=4200
emc:minecraft:dirt=1`;

inputText.value = SAMPLE_INPUT;

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle("error", isError);
}

function normalizeId(raw, fallbackNamespace) {
  if (raw == null) return null;
  let id = String(raw).trim();
  if (!id) return null;

  id = id.replace(/^["']|["']$/g, "");
  id = id.replace(/^item:/i, "");
  id = id.replace(/^block:/i, "");
  id = id.replace(/^minecraft:/i, "minecraft:");

  if (id.startsWith("#")) {
    const tagId = normalizeId(id.slice(1), fallbackNamespace);
    return tagId ? `#${tagId}` : null;
  }

  if (/^[a-z0-9_.-]+$/i.test(id)) {
    return `${fallbackNamespace || "minecraft"}:${id.toLowerCase()}`;
  }

  if (!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/i.test(id)) {
    return null;
  }

  return id.toLowerCase();
}

function normalizeValue(raw) {
  if (raw == null || raw === "") return null;
  const text = String(raw).trim().replace(/,/g, "");
  if (!/^-?\d+(\.\d+)?$/.test(text)) return null;
  const number = Number(text);
  if (!Number.isFinite(number)) return null;
  return Math.trunc(number);
}

function addEntry(result, rawKey, rawValue, fallbackNamespace) {
  const id = normalizeId(rawKey, fallbackNamespace);
  const value = normalizeValue(rawValue);

  if (!id || value == null) {
    result.skipped += 1;
    return;
  }

  if (dropZeroes.checked && value <= 0) {
    result.skipped += 1;
    return;
  }

  if (id.startsWith("#")) {
    result.tags.set(id.slice(1), value);
  } else {
    result.items.set(id, value);
  }
}

function parseJsonInput(text, fallbackNamespace) {
  const result = emptyResult();
  const parsed = JSON.parse(text);

  function visit(node, parentKey = null) {
    if (Array.isArray(node)) {
      node.forEach((entry) => visit(entry, parentKey));
      return;
    }

    if (!node || typeof node !== "object") return;

    const directId = node.item || node.id || node.key || node.name || node.registryName || node.registry_name;
    const directValue = node.emc ?? node.value ?? node.emcValue ?? node.emc_value;
    if (directId != null && directValue != null) {
      addEntry(result, directId, directValue, fallbackNamespace);
    }

    for (const [key, value] of Object.entries(node)) {
      if (key === "item" || key === "id" || key === "key" || key === "name") continue;
      if (typeof value === "number" || typeof value === "string") {
        const normalized = normalizeId(key, fallbackNamespace);
        if (normalized) addEntry(result, key, value, fallbackNamespace);
        continue;
      }

      if (value && typeof value === "object") {
        const nestedValue = value.emc ?? value.value ?? value.emcValue ?? value.emc_value;
        if (nestedValue != null) {
          addEntry(result, key, nestedValue, fallbackNamespace);
        }
        visit(value, key);
      }
    }
  }

  visit(parsed);
  return result;
}

function parseLineInput(text, fallbackNamespace) {
  const result = emptyResult();
  let section = "items";
  const vanillaSettings = new Set([
    "emc_on_hud",
    "private_emc",
    "creative_items",
    "difficulty",
    "mode"
  ]);

  for (const originalLine of text.split(/\r?\n/)) {
    let line = originalLine.trim();
    if (!line) continue;

    if (line.startsWith("#") && !/^#[a-z0-9_.-]+:[a-z0-9_./-]+/i.test(line)) continue;
    line = line.replace(/\s+#.*$/, "");
    if (!line) continue;

    const sectionMatch = line.match(/^(items|tags|values|custom_emc|custom-emc)\s*:\s*$/i);
    if (sectionMatch) {
      section = sectionMatch[1].toLowerCase() === "tags" ? "tags" : "items";
      continue;
    }

    line = line.replace(/^[SIDB]:/i, "");
    const settingMatch = line.match(/^([a-z_][a-z0-9_]*)\s*[:=]/i);
    if (settingMatch && vanillaSettings.has(settingMatch[1].toLowerCase())) {
      continue;
    }

    let match = line.match(/^["']?([^"'=\s]+)["']?\s*[:=]\s*["']?(-?\d+(?:\.\d+)?)["']?\s*[,;]?$/);
    if (!match) {
      match = line.match(/^["']?([^"'\s]+)["']?\s+["']?(-?\d+(?:\.\d+)?)["']?\s*[,;]?$/);
    }

    if (!match) {
      result.skipped += 1;
      continue;
    }

    let key = match[1];
    if (/^emc:/i.test(key)) {
      key = key.replace(/^emc:/i, "");
    }
    key = section === "tags" && !key.startsWith("#") ? `#${key}` : key;
    addEntry(result, key, match[2], fallbackNamespace);
  }

  return result;
}

function emptyResult() {
  return {
    items: new Map(),
    tags: new Map(),
    skipped: 0
  };
}

function mergeResults(primary, secondary) {
  secondary.items.forEach((value, key) => primary.items.set(key, value));
  secondary.tags.forEach((value, key) => primary.tags.set(key, value));
  primary.skipped += secondary.skipped;
  return primary;
}

function parseInput(text) {
  const fallbackNamespace = namespaceFallback.value.trim() || "minecraft";
  const mode = sourceFormat.value;

  if (mode === "dissolver") {
    return parseLineInput(text, fallbackNamespace);
  }

  if (mode === "projecte" || mode === "vanillaemc" || mode === "auto") {
    const trimmed = text.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return mergeResults(parseJsonInput(trimmed, fallbackNamespace), parseLineInput(text, fallbackNamespace));
      } catch (error) {
        if (mode !== "auto") throw error;
      }
    }
  }

  return parseLineInput(text, fallbackNamespace);
}

function yamlEscapeKey(key) {
  return /^[a-z0-9_.-]+:[a-z0-9_./-]+$/i.test(key) ? key : JSON.stringify(key);
}

function yamlSection(name, values) {
  const entries = [...values.entries()];
  if (sortOutput.checked) entries.sort(([a], [b]) => a.localeCompare(b));
  if (entries.length === 0) return `${name}: {}\n`;
  return `${name}:\n${entries.map(([key, value]) => `  ${yamlEscapeKey(key)}: ${value}`).join("\n")}\n`;
}

function buildYaml(result) {
  const lines = [];

  if (includeHeader.checked) {
    lines.push("# Generated by the Dissolver Enhanced EMC Config Converter.");
    lines.push("# Review values before using them in a pack or server.");
  }

  lines.push("schema: 1");

  if (includeHeader.checked) {
    lines.push("");
    lines.push("# Save as config/dissolver-enhanced/emc-overrides.yaml");
  }

  lines.push("");
  lines.push(yamlSection("items", result.items).trimEnd());

  if (result.tags.size > 0) {
    lines.push("");
    lines.push(yamlSection("tags", result.tags).trimEnd());
  }

  return `${lines.join("\n")}\n`;
}

function updateSummary(result) {
  summaryEl.innerHTML = "";
  for (const text of [
    `Items: ${result.items.size}`,
    `Tags: ${result.tags.size}`,
    `Skipped: ${result.skipped}`
  ]) {
    const span = document.createElement("span");
    span.textContent = text;
    summaryEl.appendChild(span);
  }
}

function convert() {
  try {
    const result = parseInput(inputText.value);
    outputText.value = buildYaml(result);
    updateSummary(result);
    setStatus("Converted");
  } catch (error) {
    setStatus("Input error", true);
    outputText.value = `# Could not convert input.\n# ${error.message}\n`;
  }
}

fileInput.addEventListener("change", async () => {
  const file = fileInput.files && fileInput.files[0];
  if (!file) return;
  inputText.value = await file.text();
  setStatus(file.name);
  convert();
});

convertButton.addEventListener("click", convert);

copyButton.addEventListener("click", async () => {
  await navigator.clipboard.writeText(outputText.value);
  setStatus("Copied");
});

downloadButton.addEventListener("click", () => {
  const blob = new Blob([outputText.value], { type: "text/yaml" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = "emc-overrides.yaml";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
});

for (const control of [sourceFormat, namespaceFallback, sortOutput, dropZeroes, includeHeader]) {
  control.addEventListener("change", convert);
}

convert();
