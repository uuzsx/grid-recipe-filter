package io.github.gridrecipefilter.mixin;
import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.client.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
 @Shadow protected RecipeBookMenu<?,?> menu;
 @Shadow protected Minecraft minecraft;
 @Shadow private EditBox searchBox;
 @Shadow private int width,height,xOffset;
 @Shadow @Final protected GhostRecipe ghostRecipe;
 @Shadow public abstract boolean isVisible();
 @Shadow public abstract void setupGhostRecipe(RecipeHolder<?> r,List<Slot> slots);
 @Shadow private void updateStackedContents(){throw new AssertionError();}
 @Shadow private void updateTabs(){throw new AssertionError();}
 @Unique private Button gridRecipeFilter$button;
 @Unique private Set<Item> gridRecipeFilter$observed=Set.of();
 @Unique private boolean gridRecipeFilter$enabled,gridRecipeFilter$all;
 @Inject(method="initVisuals",at=@At("TAIL"))
 private void gridRecipeFilter$init(CallbackInfo ci){
  if(!GridFilterState.crafting(menu))return;
  searchBox.setWidth(59);
  gridRecipeFilter$button=Button.builder(Component.empty(),b->gridRecipeFilter$toggle())
   .bounds((width-147)/2-xOffset+88,(height-166)/2+12,18,16)
   .createNarration(ignored->Component.translatable("grid_recipe_filter.narration",Component.translatable(GridRecipeFilter.ENABLED.get()?"grid_recipe_filter.on":"grid_recipe_filter.off"))).build();
  gridRecipeFilter$observed=GridFilterState.snapshot(menu);
  gridRecipeFilter$enabled=GridRecipeFilter.ENABLED.get();gridRecipeFilter$all=CraftingCatalog.enabled();
  gridRecipeFilter$buttonText();
 }
 @Unique private void gridRecipeFilter$buttonText(){
  if(gridRecipeFilter$button==null)return;
  boolean enabled=GridRecipeFilter.ENABLED.get();
  gridRecipeFilter$button.setMessage(Component.translatable("grid_recipe_filter.button").withStyle(enabled?ChatFormatting.GREEN:ChatFormatting.GRAY));
  gridRecipeFilter$button.setTooltip(Tooltip.create(Component.translatable(enabled?"grid_recipe_filter.tooltip.enabled":"grid_recipe_filter.tooltip.disabled")
   .append("\n").append(Component.translatable(CraftingCatalog.enabled()?"grid_recipe_filter.catalog.full":"grid_recipe_filter.catalog.known"))));
 }
 @Unique private void gridRecipeFilter$toggle(){GridRecipeFilter.toggle();searchBox.setFocused(false);updateStackedContents();updateTabs();gridRecipeFilter$buttonText();}
 @Inject(method="tick",at=@At("TAIL"))
 private void gridRecipeFilter$track(CallbackInfo ci){
  if(!isVisible()||!GridFilterState.crafting(menu))return;
  var next=GridFilterState.snapshot(menu);boolean enabled=GridRecipeFilter.ENABLED.get(),all=CraftingCatalog.enabled();
  if(!next.equals(gridRecipeFilter$observed)||enabled!=gridRecipeFilter$enabled||all!=gridRecipeFilter$all){
   gridRecipeFilter$observed=next;gridRecipeFilter$enabled=enabled;gridRecipeFilter$all=all;
   updateStackedContents();updateTabs();gridRecipeFilter$buttonText();
  }
 }
 @Inject(method="render",at=@At("TAIL"))
 private void gridRecipeFilter$render(GuiGraphics g,int x,int y,float delta,CallbackInfo ci){
  if(isVisible()&&gridRecipeFilter$button!=null){
   // Match the old recipe book's depth so the added control appears above its panel.
   g.pose().pushPose();g.pose().translate(0.0F,0.0F,100.0F);
   gridRecipeFilter$button.render(g,x,y,delta);g.pose().popPose();
  }
 }
 @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$click(double x,double y,int button,CallbackInfoReturnable<Boolean> cir){
  if(isVisible()&&gridRecipeFilter$button!=null&&!minecraft.player.isSpectator()&&gridRecipeFilter$button.mouseClicked(x,y,button))cir.setReturnValue(true);
 }
 @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$key(int key,int scan,int mods,CallbackInfoReturnable<Boolean> cir){
  if(isVisible()&&gridRecipeFilter$button!=null&&!minecraft.player.isSpectator()&&key==GLFW.GLFW_KEY_G&&(mods&GLFW.GLFW_MOD_CONTROL)!=0){gridRecipeFilter$toggle();cir.setReturnValue(true);}
 }
 @Redirect(method="mouseClicked",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePlaceRecipe(ILnet/minecraft/world/item/crafting/RecipeHolder;Z)V"))
 private void gridRecipeFilter$place(MultiPlayerGameMode mode,int containerId,RecipeHolder<?> recipe,boolean maximum){
  if(GridFilterState.crafting(menu)&&CraftingCatalog.enabled()&&(CraftingCatalog.locked(recipe)||!recipe.value().canCraftInDimensions(menu.getGridWidth(),menu.getGridHeight()))){
   ghostRecipe.clear();setupGhostRecipe(recipe,menu.slots);
  }else mode.handlePlaceRecipe(containerId,recipe,maximum);
 }
 @Inject(method="setupGhostRecipe",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$oversize(RecipeHolder<?> recipe,List<Slot> slots,CallbackInfo ci){
  if(GridFilterState.crafting(menu)&&!recipe.value().canCraftInDimensions(menu.getGridWidth(),menu.getGridHeight())){
   ghostRecipe.clear();ghostRecipe.setRecipe(recipe);var slot=slots.get(menu.getResultSlotIndex());
   ghostRecipe.addIngredient(Ingredient.of(recipe.value().getResultItem(minecraft.level.registryAccess())),slot.x,slot.y);
   minecraft.gui.setOverlayMessage(Component.translatable("grid_recipe_filter.requires_workbench"),false);ci.cancel();
  }
 }
 @Unique private boolean gridRecipeFilter$occupied(GhostRecipe.GhostIngredient ingredient){
  return menu.slots.stream().anyMatch(s->s.x==ingredient.getX()&&s.y==ingredient.getY()&&s.hasItem());
 }
 @Inject(method="renderGhostRecipe",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$ghosts(GuiGraphics graphics,int left,int top,boolean big,float delta,CallbackInfo ci){
  if(!GridFilterState.crafting(menu))return;
  var clock=(GhostRecipeAccess)ghostRecipe;if(!Screen.hasControlDown())clock.gridRecipeFilter$time(clock.gridRecipeFilter$time()+delta);
  for(int i=0;i<ghostRecipe.size();i++){
   var ingredient=ghostRecipe.get(i);if(gridRecipeFilter$occupied(ingredient))continue;
   int x=ingredient.getX()+left,y=ingredient.getY()+top;
   if(i==0&&big)graphics.fill(x-4,y-4,x+20,y+20,822018048);else graphics.fill(x,y,x+16,y+16,822018048);
   var stack=ingredient.getItem();graphics.renderFakeItem(stack,x,y);
   graphics.fill(RenderType.guiGhostRecipeOverlay(),x,y,x+16,y+16,822083583);
   if(i==0)graphics.renderItemDecorations(minecraft.font,stack,x,y);
  }
  ci.cancel();
 }
 @Inject(method="renderGhostRecipeTooltip",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$ghostTooltip(GuiGraphics graphics,int left,int top,int mouseX,int mouseY,CallbackInfo ci){
  if(!GridFilterState.crafting(menu))return;
  for(int i=0;i<ghostRecipe.size();i++){
   var ingredient=ghostRecipe.get(i);if(gridRecipeFilter$occupied(ingredient))continue;
   int x=ingredient.getX()+left,y=ingredient.getY()+top;
   if(mouseX>=x&&mouseY>=y&&mouseX<x+16&&mouseY<y+16){var stack=ingredient.getItem();graphics.renderComponentTooltip(minecraft.font,Screen.getTooltipFromItem(minecraft,stack),mouseX,mouseY,stack);}
  }
  ci.cancel();
 }
 @ModifyArg(method="updateNarration",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;findNarratableWidget(Ljava/util/List;Lnet/minecraft/client/gui/narration/NarratableEntry;)Lnet/minecraft/client/gui/screens/Screen$NarratableSearchResult;"),index=0)
 private List<NarratableEntry> gridRecipeFilter$narration(List<NarratableEntry> list){if(gridRecipeFilter$button==null)return list;var copy=new ArrayList<>(list);copy.add(gridRecipeFilter$button);return copy;}
}
