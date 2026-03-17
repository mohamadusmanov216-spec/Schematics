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
        context.fill(0, 0, width, height, 0xB0101010);

        int panelWidth = Math.min(560, width - 24);
        int panelHeight = Math.min(height - 24, 220);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        context.fill(left, top, right, bottom, 0xD0151720);
        context.fill(left, top, right, top + 18, 0xFF1C2433);
        context.fill(left, top + 18, right, top + 19, 0xFF2D3A52);

        context.drawText(textRenderer, Text.literal("SBuild • Materials"), left + 8, top + 5, 0x9FE2FF, false);
        context.drawText(
            textRenderer,
            Text.literal("Total: " + report.totalRequired() + "   Built: " + report.totalAlreadyBuilt() + "   Remaining: " + report.totalRemaining()),
            left + 8,
            top + 24,
            0xC8D3E6,
            false
        );

        int colName = left + 8;
        int colNeed = right - 170;
        int colHave = right - 85;
        int headerY = top + 40;

        context.drawText(textRenderer, Text.literal("Block"), colName, headerY, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Need"), colNeed, headerY, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Have"), colHave, headerY, 0xFFFFFF, false);
        context.fill(left + 8, headerY + 10, right - 8, headerY + 11, 0x553A4A66);

        List<MaterialRequirement> rows = report.rows();
        int y = headerY + 16;
        int maxRows = Math.max(1, (bottom - y - 12) / 11);
        for (int i = 0; i < rows.size() && i < maxRows; i++) {
            MaterialRequirement row = rows.get(i);
            int rowBg = i % 2 == 0 ? 0x221A2233 : 0x22304058;
            context.fill(left + 6, y - 1, right - 6, y + 10, rowBg);

            int needColor = row.remaining() > 0 ? 0xFF9A9A : 0x9AFF9A;
            int haveColor = row.availableInInventory() > 0 ? 0x8BFFB8 : 0xFF8B8B;

            context.drawText(textRenderer, Text.literal(row.materialKey()), colName, y, 0xE8EEFF, false);
            context.drawText(textRenderer, Text.literal(Long.toString(row.remaining())), colNeed, y, needColor, false);
            context.drawText(textRenderer, Text.literal(Long.toString(row.availableInInventory())), colHave, y, haveColor, false);
            y += 11;
        }

        if (rows.size() > maxRows) {
            context.drawText(textRenderer, Text.literal("... and " + (rows.size() - maxRows) + " more"), left + 8, bottom - 11, 0x9AA9C0, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
