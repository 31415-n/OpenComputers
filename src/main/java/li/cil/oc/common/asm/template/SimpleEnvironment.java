package li.cil.oc.common.asm.template;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// This is a template implementation of methods injected into classes that are
// marked for component functionality. These methods will be copied into tile
// entities marked as simple components as necessary by the class transformer.
@SuppressWarnings("unused")
public abstract class SimpleEnvironment extends BlockEntity implements SimpleComponentImpl {
    
    protected SimpleEnvironment(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override
    public Node node() {
        return StaticSimpleEnvironment.node(this);
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }

    // These are always injected, after possibly existing versions have been
    // renamed to the below variants from the SimpleComponentImpl interface.
    // This allows transparent wrapping of already present implementations,
    // instead of plain overwriting them.

    @Override
    public void onLoad() {
        StaticSimpleEnvironment.validate(this);
    }

    @Override
    public void setRemoved() {
        StaticSimpleEnvironment.invalidate(this);
        super.setRemoved();
    }

    public void onChunkUnloaded() {
        StaticSimpleEnvironment.onChunkUnload(this);
    }

    @Override
    public void load(CompoundTag nbt) {
        StaticSimpleEnvironment.readFromNBT(this, nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        StaticSimpleEnvironment.writeToNBT(this, nbt);
    }

    // The following methods are only injected if their real versions do not
    // exist in the class we're injecting into. Otherwise their real versions
    // are renamed to these variations, which simply delegate to the parent.
    // This way they are always guaranteed to be present, so we can simply call
    // them through an interface, and need no runtime reflection.

    public void validate_OpenComputers() {
        super.onLoad();
    }

    public void invalidate_OpenComputers() {
        super.setRemoved();
    }

    public void onChunkUnload_OpenComputers() {
        // In 1.20.1, chunk unloading cleanup is handled in setRemoved()
        // or through chunk events. For OpenComputers network cleanup,
        // we ensure nodes are properly disconnected when chunk unloads.
        // This is now handled automatically in setRemoved() method.
    }

    public void readFromNBT_OpenComputers(CompoundTag nbt) {
        super.load(nbt);
    }

    public CompoundTag writeToNBT_OpenComputers(CompoundTag nbt) {
        super.saveAdditional(nbt);
        return nbt;
    }
}
