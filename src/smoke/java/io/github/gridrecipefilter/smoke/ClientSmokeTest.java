package io.github.gridrecipefilter.smoke;
import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.client.CraftingCatalog;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid=GridRecipeFilter.MOD_ID,value=Dist.CLIENT)
public final class ClientSmokeTest {
 private static final List<String> checks=new ArrayList<>();
 private static int stage,delay;private static final long started=System.nanoTime();
 private static RecipeBookComponent component;private static RecipeBookMenu<?,?> menu;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();try{
   if(stage==99)return;if((System.nanoTime()-started)/1_000_000_000>240)throw new AssertionError("Timeout "+stage);
   if(stage==0&&mc.getOverlay()==null&&mc.screen!=null){
    stage=1;mc.options.pauseOnLostFocus=false;mc.options.languageCode="zh_cn";
    mc.createWorldOpenFlows().createFreshLevel("grid-filter-smoke-"+System.currentTimeMillis(),
     new LevelSettings("Grid Filter Smoke",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
     new WorldOptions(42,false,false),WorldPresets::createNormalWorldDimensions,mc.screen);
   }else if(stage==1&&mc.player!=null&&mc.level!=null&&mc.getConnection().getRecipeManager().byKey(id("smoke_frame")).isPresent()){
    test(mc,false);test(mc,true);stage=2;delay=12;
   }else if(stage==2&&--delay<=0){
    Screenshot.grab(mc.gameDirectory,"mod-locked-workbench.png",mc.getMainRenderTarget(),ignored->{});
    mc.screen.onClose();stage=3;
    mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();server.getCommands().performPrefixedCommand(player.createCommandSourceStack(),"recipe give @s *");});
   }else if(stage==3&&mc.player.getRecipeBook().contains(id("smoke_frame"))){
    open(mc,false);grid(SmokeFixtures.INGOT.get());var plate=fixture(mc,"smoke_plate");
    check(!CraftingCatalog.locked(plate),"Awarded recipe is recognized as unlocked");
    menu.getSlot(1).set(new ItemStack(SmokeFixtures.INGOT.get(),64));component.slotClicked(menu.getSlot(1));
    check(collection(mc,plate).isCraftable(plate),"Unlocked sufficient recipe becomes craftable");
    GridRecipeFilter.ALL_CRAFTING_RECIPES.set(false);CraftingCatalog.refresh();
    check(selected(mc).contains(plate.id()),"Known-only mode retains actually unlocked recipes");stage=4;delay=12;
   }else if(stage==4&&--delay<=0){Screenshot.grab(mc.gameDirectory,"inventory-filter.png",mc.getMainRenderTarget(),ignored->{});stage=5;delay=12;}
   else if(stage==5&&--delay<=0)finish(mc,null);
  }catch(Throwable failure){failure.printStackTrace();finish(mc,failure);}
 }
 private static void open(Minecraft mc,boolean table){
  GridRecipeFilter.ENABLED.set(true);GridRecipeFilter.ALL_CRAFTING_RECIPES.set(true);mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING,true);
  menu=table?new CraftingMenu(1,mc.player.getInventory()):mc.player.inventoryMenu;mc.player.containerMenu=menu;
  mc.setScreen(table?new CraftingScreen((CraftingMenu)menu,mc.player.getInventory(),Component.translatable("container.crafting")):new InventoryScreen(mc.player));
  component=((RecipeUpdateListener)mc.screen).getRecipeBookComponent();CraftingCatalog.refresh();check(component.isVisible(),"Book opens "+(table?"3x3":"2x2"));
 }
 private static void test(Minecraft mc,boolean table)throws Exception{
  open(mc,table);var plate=fixture(mc,"smoke_plate");var frame=fixture(mc,"smoke_frame");var tag=fixture(mc,"smoke_tag");var expected=Set.of(plate.id(),frame.id(),tag.id());
  check(!mc.player.getRecipeBook().contains(plate)&&!mc.player.getRecipeBook().contains(frame)&&!mc.player.getRecipeBook().contains(tag),"Full vanilla catalog does not grant fixture recipes");
  grid(SmokeFixtures.INGOT.get());check(selected(mc).equals(expected),"Mod ingot matches every locked crafting recipe including 3x3");
  menu.getSlot(1).set(new ItemStack(SmokeFixtures.INGOT.get(),64));component.tick();check(selected(mc).equals(expected),"Counts do not affect filtering");
  var plates=collection(mc,plate);var stacked=new StackedContents();menu.fillCraftSlotsStackedContents(stacked);component.slotClicked(menu.getSlot(1));
  check(stacked.canCraft(plate.value(),null)&&!plates.isCraftable(plate),"Locked but sufficient recipe stays red");
  check(plates.getRecipes(true).isEmpty()&&!plates.getRecipes(false).isEmpty(),"Only-craftable excludes locked recipes");
  grid(SmokeFixtures.ALLOY.get());check(selected(mc).equals(Set.of(tag.id())),"Mod-defined tag matches second registered material");
  grid(SmokeFixtures.INGOT.get(),Items.STICK);check(selected(mc).equals(Set.of(tag.id())),"Multiple materials all participate in selected recipe");
  grid(SmokeFixtures.PLATE.get());check(selected(mc).isEmpty(),"Output is not an input");
  grid(Items.OAK_PLANKS);check(!selected(mc).isEmpty(),"Vanilla tags resolve");
  grid(Items.COAL,Items.CHARCOAL);check(!selected(mc).contains(ResourceLocation.withDefaultNamespace("torch")),"Alternative fuels cannot share one ingredient slot");
  grid(Items.COAL,Items.STICK);check(selected(mc).contains(ResourceLocation.withDefaultNamespace("torch")),"Vanilla coal and stick match torch");
  grid(SmokeFixtures.INGOT.get());var search=(EditBox)field("searchBox");search.setValue("smoke_plate");invoke("checkSearchStringUpdate");
  var page=field("recipeBookPage");var f=page.getClass().getDeclaredField("recipeCollections");f.setAccessible(true);
  @SuppressWarnings("unchecked") var found=(List<RecipeCollection>)f.get(page);check(found.size()==1&&found.getFirst().getRecipes().contains(plate),"Native text search includes locked mod recipe");
  grid(SmokeFixtures.ALLOY.get());check(search.getValue().equals("smoke_plate"),"Grid refresh keeps search text");search.setValue("");invoke("checkSearchStringUpdate");grid(SmokeFixtures.INGOT.get());
  var button=(Button)field("gridRecipeFilter$button");check(component.mouseClicked(button.getX()+5,button.getY()+5,0)&&!GridRecipeFilter.ENABLED.get()&&selected(mc).size()>3,"Mouse material toggle restores all recipes");
  check(component.keyPressed(GLFW.GLFW_KEY_G,0,GLFW.GLFW_MOD_CONTROL)&&GridRecipeFilter.ENABLED.get()&&selected(mc).equals(expected),"Ctrl+G toggles the filter");
  GridRecipeFilter.ALL_CRAFTING_RECIPES.set(false);CraftingCatalog.refresh();component.tick();check(selected(mc).isEmpty(),"Known-only excludes locked recipes");
  GridRecipeFilter.ALL_CRAFTING_RECIPES.set(true);CraftingCatalog.refresh();component.tick();check(selected(mc).equals(expected),"Full catalog restores locked entries without granting recipes");
  place(mc,plate);check(ghost().size()==2,"Locked small recipe uses native ghosts");place(mc,frame);check(ghost().size()==(table?9:1),"3x3 preview fits workbench and safely shows output only in inventory");
  check(menu.getSlot(1).getItem().is(SmokeFixtures.INGOT.get())&&mc.player.containerMenu==menu,"Preview preserves container and real input");
  check(!mc.player.getRecipeBook().contains(frame)&&mc.getSingleplayerServer().submit(()->!mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getRecipeBook().contains(frame.id())).get(),"Preview does not unlock client or server recipe");
  grid();check(selected(mc).size()>100,"Empty grid restores full catalog");grid(SmokeFixtures.INGOT.get());
  var rb=new RecipeButton();rb.init(plates,(RecipeBookPage)page);check(rb.getTooltipText().equals(Screen.getTooltipFromItem(mc,plate.value().getResultItem(mc.level.registryAccess()))),"No extra yellow locked tooltip");
  menu.getSlot(1).set(new ItemStack(SmokeFixtures.INGOT.get(),64));place(mc,frame);
  if(table)testGhosts(mc,frame);
 }
 private static void testGhosts(Minecraft mc,RecipeHolder<?> frame)throws Exception{
  var g=new RecordingGraphics(mc);component.renderGhostRecipe(g,0,0,true,1);check(g.icons.size()==8&&!g.hasGhostAt(menu.getSlot(1)),"Real input has no preview icon or red overlay");check(menu.getSlot(1).getItem().getCount()==64,"Real count 64 stays unchanged");
  menu.getSlot(2).set(new ItemStack(Items.DIAMOND,12));g=new RecordingGraphics(mc);component.renderGhostRecipe(g,0,0,true,1);check(g.icons.size()==7&&!g.hasGhostAt(menu.getSlot(2))&&menu.getSlot(2).getItem().getCount()==12,"New real item immediately hides ghost");
  component.renderTooltip(g,0,0,menu.getSlot(2).x+3,menu.getSlot(2).y+3);check(g.tooltips==0,"Occupied slot retains real tooltip");
  menu.getSlot(2).set(ItemStack.EMPTY);g=new RecordingGraphics(mc);component.renderGhostRecipe(g,0,0,true,1);check(g.icons.size()==8&&g.hasGhostAt(menu.getSlot(2)),"Empty slot restores ghost");component.renderTooltip(g,0,0,menu.getSlot(2).x+3,menu.getSlot(2).y+3);check(g.tooltips==1,"Empty slot shows ingredient tooltip");
  menu.getSlot(0).set(new ItemStack(Items.APPLE,3));g=new RecordingGraphics(mc);component.renderGhostRecipe(g,0,0,true,1);check(g.icons.size()==7&&!g.hasGhostAt(menu.getSlot(0)),"Real output is not covered");
  menu.getSlot(0).set(ItemStack.EMPTY);g=new RecordingGraphics(mc);component.renderGhostRecipe(g,0,0,true,1);check(g.icons.size()==8&&ghost().size()==9,"Empty output restores ghost");
  check(java.util.stream.IntStream.range(0,ghost().size()).noneMatch(i->{try{return ghost().get(i).getX()==menu.getSlot(5).x&&ghost().get(i).getY()==menu.getSlot(5).y;}catch(Exception e){throw new RuntimeException(e);}}),"Shaped recipe retains empty center");
  component.slotClicked(menu.getSlot(1));check(ghost().size()==0,"Clicking slot clears ghosts");place(mc,frame);
 }
 private static void place(Minecraft mc,RecipeHolder<?> recipe)throws Exception{
  ghost().clear();
  var page=field("recipeBookPage");var f=page.getClass().getDeclaredField("buttons");f.setAccessible(true);
  @SuppressWarnings("unchecked") var buttons=(List<RecipeButton>)f.get(page);
  var button=buttons.stream().filter(b->b.visible&&b.getCollection().getRecipes().contains(recipe)).findFirst().orElseThrow();
  if(!component.mouseClicked(button.getX()+4,button.getY()+4,0))throw new AssertionError("Recipe click was not handled: "+recipe.id());
 }
 private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(GridRecipeFilter.MOD_ID,name);}
 private static RecipeHolder<?> fixture(Minecraft mc,String name){return mc.getConnection().getRecipeManager().byKey(id(name)).orElseThrow();}
 private static RecipeCollection collection(Minecraft mc,RecipeHolder<?> r){return mc.player.getRecipeBook().getCollections().stream().filter(c->c.getRecipes().contains(r)).findFirst().orElseThrow();}
 private static Set<ResourceLocation> selected(Minecraft mc){return mc.player.getRecipeBook().getCollection(RecipeBookCategories.CRAFTING_SEARCH).stream().flatMap(c->c.getRecipes(false).stream()).map(RecipeHolder::id).collect(Collectors.toSet());}
 private static Object field(String name)throws Exception{var f=RecipeBookComponent.class.getDeclaredField(name);f.setAccessible(true);return f.get(component);}
 private static void invoke(String name)throws Exception{var m=RecipeBookComponent.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(component);}
 private static GhostRecipe ghost()throws Exception{return (GhostRecipe)field("ghostRecipe");}
 private static void grid(Item... items){for(int i=1;i<=menu.getGridWidth()*menu.getGridHeight();i++)menu.getSlot(i).set(i<=items.length?new ItemStack(items[i-1]):ItemStack.EMPTY);component.tick();}
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks.add("PASS: "+message);}
 private static void finish(Minecraft mc,Throwable failure){stage=99;try{var result=failure==null?"PASS: "+checks.size()+" integration checks":"FAIL: "+failure;Files.writeString(Path.of(mc.gameDirectory.toString(),"smoke-result.txt"),result+"\n"+String.join("\n",checks));System.out.println("GRID_FILTER_SMOKE "+result);}catch(Exception e){e.printStackTrace();}mc.stop();}
 private static final class RecordingGraphics extends GuiGraphics{
  final List<int[]> icons=new ArrayList<>(),rectangles=new ArrayList<>();int tooltips;RecordingGraphics(Minecraft mc){super(mc,mc.renderBuffers().bufferSource());}
  @Override public void fill(int x0,int y0,int x1,int y1,int color){rectangles.add(new int[]{x0,y0,x1,y1});}
  @Override public void fill(RenderType type,int x0,int y0,int x1,int y1,int color){rectangles.add(new int[]{x0,y0,x1,y1});}
  @Override public void renderFakeItem(ItemStack stack,int x,int y){icons.add(new int[]{x,y});}
  @Override public void renderItemDecorations(Font font,ItemStack stack,int x,int y){}
  @Override public void renderComponentTooltip(Font font,List<? extends FormattedText> lines,int x,int y,ItemStack stack){tooltips++;}
  boolean hasGhostAt(Slot s){return icons.stream().anyMatch(p->p[0]==s.x&&p[1]==s.y)||rectangles.stream().anyMatch(p->p[0]<=s.x&&p[1]<=s.y&&p[2]>=s.x+16&&p[3]>=s.y+16);}
 }
}
