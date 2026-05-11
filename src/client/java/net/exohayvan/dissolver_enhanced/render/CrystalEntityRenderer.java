package net.exohayvan.dissolver_enhanced.render;

import org.joml.Quaternionf;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.entity.CrystalEntity;

public class CrystalEntityRenderer extends EntityRenderer<CrystalEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(DissolverEnhanced.MOD_ID, "textures/entity/crystal_entity.png");
	private static final RenderType CRYSTAL_RENDER = RenderType.entityCutoutNoCull(TEXTURE);
	private static final float SINE_45_DEGREES = (float)Math.sin(Math.PI / 4);
	// private static final String GLASS = "glass";
	// private static final String BASE = "base";
	private final ModelPart core;
	private final ModelPart frame;
	// private final ModelPart bottom;
	private final float SCALE = 0.8F;
	private final float SPEED = 0.5F;

	public CrystalEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		ModelPart modelPart = context.bakeLayer(ModelLayers.END_CRYSTAL);
		this.frame = modelPart.getChild("glass");
		this.core = modelPart.getChild(PartNames.CUBE);
		// this.bottom = modelPart.getChild("base");
		
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.0F;
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition modelPartData = modelData.getRoot();
		modelPartData.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		modelPartData.addOrReplaceChild(PartNames.CUBE, CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		return LayerDefinition.create(modelData, 64, 32);
	}

	public void render(CrystalEntity crystalEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i) {
		float customSpeed = SPEED;
		if (crystalEntity.isPowered()) customSpeed = 4.0F;

		matrixStack.pushPose();
		float j = ((float)crystalEntity.crystalAge + g) * customSpeed;
		VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(CRYSTAL_RENDER);
		matrixStack.pushPose();
		
		int k = OverlayTexture.NO_OVERLAY;
		
		matrixStack.scale(SCALE, SCALE, SCALE);
		for (int index = 0; index < 3; index++) {
			matrixStack.scale(0.8F, 0.8F, 0.8F);
			matrixStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
			matrixStack.mulPose(Axis.YP.rotationDegrees(j));
			this.frame.render(matrixStack, vertexConsumer, i, k);
		}

		// matrixStack.scale(0.875F, 0.875F, 0.875F);
		// matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
		// matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
		// this.frame.render(matrixStack, vertexConsumer, i, k);

		matrixStack.scale(0.7F, 0.7F, 0.7F);
		// // matrixStack.push();
		// // matrixStack.multiply(test, 0.0F, 0.0F, 0.0F);
		// // matrixStack.multiply(test2, 0.0F, 0.0F, 0.0F);
		// for (int index = 0; index < 3; index++) {
		// 	// Quaternionf newStack = new Quaternionf(matrixStack.peek());
		// 	// matrixStack.scale(index, k, index);
		// 	matrixStack.push();
		// 	// float ANGLE = SINE_45_DEGREES * (index);
		// 	Quaternionf test = new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES);
		// 	Quaternionf test2 = RotationAxis.POSITIVE_Y.rotationDegrees(j);
		// 	// Quaternionf test2 = RotationAxis.POSITIVE_Y.rotationDegrees(j + (index * 30));
		// 	// Quaternionf test3 = RotationAxis.POSITIVE_X.rotationDegrees(j + (index * 30));
		// 	matrixStack.multiply(test, 0.0F + (index * 2), 0.0F, 0.0F);
		// 	matrixStack.multiply(test2, 0.0F + (index * 2), 0.0F, 0.0F);
		// 	// matrixStack.multiply(test3, 0.0F, 0.0F, 0.0F);
		// 	this.core.render(matrixStack, vertexConsumer, i, k);
		// 	matrixStack.pop();
		// }

		matrixStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
		matrixStack.mulPose(Axis.YP.rotationDegrees(j));
		this.core.render(matrixStack, vertexConsumer, i, k);

		matrixStack.popPose();
		matrixStack.popPose();

		super.render(crystalEntity, f, g, matrixStack, vertexConsumerProvider, i);
	}

	public ResourceLocation getTextureLocation(CrystalEntity crystalEntity) {
		return TEXTURE;
	}

	public boolean shouldRender(CrystalEntity crystalEntity, Frustum frustum, double d, double e, double f) {
		return super.shouldRender(crystalEntity, frustum, d, e, f); // || crystalEntity.getBeamTarget() != null;
	}
}
