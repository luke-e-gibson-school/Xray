package fr.atesab.xray;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XrayMode implements SideRenderer, IColorObject {
	@FunctionalInterface
	public static interface Viewer {
		public boolean shouldRenderSide(boolean blockInList, BlockState adjacentState, BlockView blockState,
				BlockPos blockAccess, Direction pos);
	}

	public static enum ViewMode {
		/**
		 * Default mode, like in Xray and Redstone mode
		 */
		EXCLUSIVE((il, v1, v2, v3, v4) -> il),
		/**
		 * Inclusive mode, like in Cave Mode
		 */
		INCLUSIVE((il, v1, reader, pos, face) -> !il && reader.getBlockState(pos.offset(face)).isAir());

		private Viewer viewer;

		private ViewMode(Viewer viewer) {
			this.viewer = viewer;
		}

		public Viewer getViewer() {
			return viewer;
		}
	}

	private static final List<XrayMode> MODES = Lists.newArrayList();
	private List<Block> blocks;
	private final List<Block> defaultBlocks;
	private boolean enabled;
	private KeyBinding key;
	private final String name;
	private final int color;
	private ViewMode viewMode;
	private static int colorCursor = -1;
	private static final int[] COLORS = { 0xff00ffff, 0xffff0000, 0xffffff00, 0xffff00ff, 0xff7aff00, 0xffff7a00,
			0xff00ff7a, 0xffff007a, 0xff7a00ff, 0xff7a7aff, 0xff7aff7a, 0xffff7a7a };
	public static final String CUSTOM_PREFIX = "Custom_";

	static int nextColor() {
		return COLORS[colorCursor = (colorCursor + 1) % COLORS.length];
	}

	public XrayMode(String name, int keyCode, ViewMode viewMode, @Nullable Block... defaultBlocks) {
		this.name = name;
		this.color = nextColor();
		this.enabled = false;
		this.key = new KeyBinding("x13.mod." + name, InputUtil.Type.KEYSYM, keyCode, "key.categories.xray");
		this.viewMode = viewMode;
		if (defaultBlocks != null) {
			this.blocks = Lists.newArrayList(defaultBlocks);
			this.defaultBlocks = Arrays.asList(defaultBlocks);
		} else {
			this.blocks = Lists.newArrayList();
			this.defaultBlocks = Arrays.asList();
		}
		MODES.add(this);
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public List<Block> getDefaultBlocks() {
		return defaultBlocks;
	}

	public void reset() {
		setBlocks(new ArrayList<>(getDefaultBlocks()));
	}

	public int getColor() {
		return color;
	}

	public KeyBinding getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getNameTranslate() {
		return name.startsWith(CUSTOM_PREFIX) ? name : I18n.translate("x13.mod." + name);
	}

	@Override
	public String getModeName() {
		return getNameTranslate();
	}

	public ViewMode getViewMode() {
		return viewMode;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	public void setViewMode(ViewMode viewMode) {
		this.viewMode = viewMode;
	}

	public boolean toggleKey() {
		if (key.wasPressed()) {
			toggle();
			return true;
		}
		return false;
	}

	public void setConfig(String[] data) {
		blocks.clear();
		for (String d : data) {
			Block b = Registry.BLOCK.get(new Identifier(d));
			if (!b.equals(Blocks.AIR))
				blocks.add(b);
		}
	}

	public void toggle() {
		toggle(!enabled);
	}

	public void toggle(boolean enable) {
		toggle(enable, true);
	}

	public void toggle(boolean enable, boolean reloadRenderers) {
		XrayMain mod = XrayMain.getMod();
		XrayMode old = mod.getSelectedMode();
		if (old != null)
			old.toggle0(false);
		toggle0(enable);
		if (enabled)
			mod.setSelectedMode(this);
		mod.internalFullbright();
		MinecraftClient mc = MinecraftClient.getInstance();
		if (reloadRenderers)
			mc.worldRenderer.reload();
	}

	private void toggle0(boolean enable) {
		if (enabled && !enable) {
			XrayMain.getMod().setSelectedMode(null);
		}
		this.enabled = enable;
	}

	@Override
	public void shouldSideBeRendered(BlockState adjacentState, BlockView blockState, BlockPos blockAccess,
			Direction pos, CallbackInfoReturnable<Boolean> ci) {
		if (isEnabled())
			ci.setReturnValue(viewMode.getViewer().shouldRenderSide(blocks.contains(adjacentState.getBlock()),
					adjacentState, blockState, blockAccess, pos));
	}
}
