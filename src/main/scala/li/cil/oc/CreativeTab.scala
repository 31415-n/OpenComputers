package li.cil.oc

import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component

object CreativeTab {
  val OPENCOMPUTERS: CreativeModeTab = CreativeModeTab.builder()
    .title(Component.translatable("itemGroup." + OpenComputers.Name))
    .icon(() => {
      val stack = api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1)
      if (stack != null) stack else ItemStack.EMPTY
    })
    .build()
}
