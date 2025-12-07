package net.vi.mobhealthindicators.fabric.config.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.vi.mobhealthindicators.config.screen.ConfigScreenHandler;

public class ModMenuScreen implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenHandler::getConfigScreen;
    }
}
