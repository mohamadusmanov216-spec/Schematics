package sbuild.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import sbuild.materials.MaterialReport;
import sbuild.materials.MaterialRequirement;

import java.util.List;

public final class MaterialListScreen extends Screen {
    private final MaterialReport report;

    public MaterialListScreen(MaterialReport report) {
        super(Text.literal("SBuild Material List"));
        this.report = report;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Avoid Screen#applyBlur double-call crashes on some Fabric screen pipelines.
        context.fill(0, 0, width, height, 0xA0101010);

        int left = 12;
        int top = 12;
        context.fill(left - 6, top - 6, width - 12, height - 12, 0xAA000000);

        context.drawText(textRenderer, Text.literal("§eSBuild • Материалы"), left, top, 0xFFFFFF, true);
        context.drawText(textRenderer, Text.literal("§7Total=" + report.totalRequired() + " Built=" + report.totalAlreadyBuilt() + " Remaining=" + report.totalRemaining()), left, top + 12, 0xFFFFFF, false);

        context.drawText(textRenderer, Text.literal("§fБлок"), left, top + 28, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("§fНужно"), left + 260, top + 28, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("§fЕсть"), left + 330, top + 28, 0xFFFFFF, false);

        List<MaterialRequirement> rows = report.rows();
        int y = top + 44;
        int maxRows = Math.max(1, (height - y - 24) / 11);
        for (int i = 0; i < rows.size() && i < maxRows; i++) {
            MaterialRequirement row = rows.get(i);
            int color = row.remaining() > 0 ? 0xFF7777 : 0x77FF77;
            context.drawText(textRenderer, Text.literal(row.materialKey()), left, y, 0xFFFFFF, false);
            context.drawText(textRenderer, Text.literal(Long.toString(row.remaining())), left + 260, y, color, false);
            context.drawText(textRenderer, Text.literal(Long.toString(row.availableInInventory())), left + 330, y, row.availableInInventory() > 0 ? 0x77FF77 : 0xFF7777, false);
            y += 11;
        }

        if (rows.size() > maxRows) {
            context.drawText(textRenderer, Text.literal("§7... ещё " + (rows.size() - maxRows) + " строк"), left, height - 24, 0xFFFFFF, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
