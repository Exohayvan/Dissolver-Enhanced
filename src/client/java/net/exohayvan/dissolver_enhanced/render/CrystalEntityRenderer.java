package net.exohayvan.dissolver_enhanced.render;

import net.exohayvan.dissolver_enhanced.entity.CrystalEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class CrystalEntityRenderer extends EntityRenderer<CrystalEntity, EntityRenderState> {
	public CrystalEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.0F;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
