package net.minecraft.client;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import de.florianmichael.viafabricplus.access.IMouseKeyboard;
import de.florianmichael.viafabricplus.event.PostGameLoadCallback;
import de.florianmichael.viafabricplus.fixes.data.ItemRegistryDiff;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import dev.vision.Vision;
import dev.tr7zw.entityculling.EntityCullingMod;
import dev.vision.events.EventTick;
import icyllis.modernui.mc.BlurHandler;
import icyllis.modernui.mc.ModernUIClient;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.UIManager;
import icyllis.modernui.mc.fabric.ModernUIFabricClient;
import me.empty.api.event.handler.EventManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.Optionull;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FileZipper;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
   static Minecraft instance;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final boolean ON_OSX = Util.getPlatform() == Util.OS.OSX;
   private static final int MAX_TICKS_PER_UPDATE = 10;
   public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("default");
   public static final ResourceLocation UNIFORM_FONT = new ResourceLocation("uniform");
   public static final ResourceLocation ALT_FONT = new ResourceLocation("alt");
   private static final ResourceLocation REGIONAL_COMPLIANCIES = new ResourceLocation("regional_compliancies.json");
   private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
   private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
   public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final long canary = Double.doubleToLongBits(Math.PI);
   private final Path resourcePackDirectory;
   private CompletableFuture<ProfileResult> profileFuture;
   private final TextureManager textureManager;
   private final DataFixer fixerUpper;
   private final VirtualScreen virtualScreen;
   private final Window window;
   private final Timer timer = new Timer(20.0F, 0L, this::getTickTargetMillis);
   private final RenderBuffers renderBuffers;
   public final LevelRenderer levelRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;
   public final ParticleEngine particleEngine;
   private final SearchRegistry searchRegistry = new SearchRegistry();
   private User user;
   public final Font font;
   public final Font fontFilterFishy;
   public final GameRenderer gameRenderer;
   public final DebugRenderer debugRenderer;
   private final AtomicReference<StoringChunkProgressListener> progressListener = new AtomicReference<>();
   public final Gui gui;
   public final Options options;
   private final HotbarManager hotbarManager;
   public final MouseHandler mouseHandler;
   public final KeyboardHandler keyboardHandler;
   private InputType lastInputType = InputType.NONE;
   public final File gameDirectory;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private final LevelStorageSource levelSource;
   private final boolean is64bit;
   private final boolean demo;
   private final boolean allowsMultiplayer;
   private final boolean allowsChat;
   private final ReloadableResourceManager resourceManager;
   private final VanillaPackResources vanillaPackResources;
   private final DownloadedPackSource downloadedPackSource;
   private final PackRepository resourcePackRepository;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final ItemColors itemColors;
   private final RenderTarget mainRenderTarget;
   private final SoundManager soundManager;
   private final MusicManager musicManager;
   public final FontManager fontManager;
   private final SplashManager splashManager;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
   private final YggdrasilAuthenticationService authenticationService;
   private MinecraftSessionService minecraftSessionService;
   private UserApiService userApiService;
   private CompletableFuture<UserApiService.UserProperties> userPropertiesFuture;
   private SkinManager skinManager;
   private final ModelManager modelManager;
   private final BlockRenderDispatcher blockRenderer;
   private final PaintingTextureManager paintingTextures;
   private final MobEffectTextureManager mobEffectTextures;
   private final GuiSpriteManager guiSprites;
   private final ToastComponent toast;
   private final Tutorial tutorial;
   private PlayerSocialManager playerSocialManager;
   private final EntityModelSet entityModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final ClientTelemetryManager telemetryManager;
   private final ProfileKeyPairManager profileKeyPairManager;
   private final RealmsDataFetcher realmsDataFetcher;
   private final QuickPlayLog quickPlayLog;
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Nullable
   public ClientLevel level;
   @Nullable
   public LocalPlayer player;
   @Nullable
   private IntegratedServer singleplayerServer;
   @Nullable
   private Connection pendingConnection;
   private boolean isLocalServer;
   @Nullable
   public Entity cameraEntity;
   @Nullable
   public Entity crosshairPickEntity;
   @Nullable
   public HitResult hitResult;
   private int rightClickDelay;
   protected int missTime;
   private volatile boolean pause;
   private float pausePartialTick;
   private long lastNanoTime = Util.getNanos();
   private long lastTime;
   private int frames;
   public boolean noRender;
   @Nullable
   public Screen screen;
   @Nullable
   private Overlay overlay;
   private boolean clientLevelTeardownInProgress;
   public Thread gameThread;
   private volatile boolean running;
   @Nullable
   private Supplier<CrashReport> delayedCrash;
   private static int fps;
   public String fpsString = "";
   private long frameTimeNs;
   public boolean wireframe;
   public boolean sectionPath;
   public boolean sectionVisibility;
   public boolean smartCull = true;
   private boolean windowActive;
   private final Queue<Runnable> progressTasks = Queues.newConcurrentLinkedQueue();
   @Nullable
   private CompletableFuture<Void> pendingReload;
   @Nullable
   private TutorialToast socialInteractionsToast;
   private ProfilerFiller profiler = InactiveProfiler.INSTANCE;
   private int fpsPieRenderTicks;
   private final ContinuousProfiler fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> {
      return this.fpsPieRenderTicks;
   });
   @Nullable
   private ProfileResults fpsPieResults;
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
   private long savedCpuDuration;
   private double gpuUtilization;
   @Nullable
   private TimerQuery.FrameProfile currentFrameProfile;
   private final Realms32BitWarningStatus realms32BitWarningStatus;
   private final GameNarrator narrator;
   private final ChatListener chatListener;
   private ReportingContext reportingContext;
   private final CommandHistory commandHistory;
   private final DirectoryValidator directoryValidator;
   private boolean gameLoadFinished;
   private final long clientStartTimeMs;
   private long clientTickCount;
   private String debugPath = "root";
   private BiFunction<EventState, TickEvent.Type, TickEvent> tickProvider;

   public Minecraft(GameConfig pGameConfig) {
      super("Client");
      instance = this;
      this.clientStartTimeMs = System.currentTimeMillis();
      this.gameDirectory = pGameConfig.location.gameDirectory;
      File file1 = pGameConfig.location.assetDirectory;
      this.resourcePackDirectory = pGameConfig.location.resourcePackDirectory.toPath();
      this.launchedVersion = pGameConfig.game.launchVersion;
      this.versionType = pGameConfig.game.versionType;
      Path path = this.gameDirectory.toPath();
      this.directoryValidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
      ClientPackSource clientpacksource = new ClientPackSource(pGameConfig.location.getExternalAssetSource(), this.directoryValidator);
      this.downloadedPackSource = new DownloadedPackSource(this, path.resolve("downloads"), pGameConfig.user);
      RepositorySource repositorysource = new FolderRepositorySource(this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator);
      this.resourcePackRepository = new PackRepository(clientpacksource, this.downloadedPackSource.createRepositorySource(), repositorysource);
      this.vanillaPackResources = clientpacksource.getVanillaPack();
      this.proxy = pGameConfig.user.proxy;
      this.authenticationService = new YggdrasilAuthenticationService(this.proxy);
      this.minecraftSessionService = this.authenticationService.createMinecraftSessionService();
      this.user = pGameConfig.user.user;
      this.profileFuture = CompletableFuture.supplyAsync(() -> {
         return this.minecraftSessionService.fetchProfile(this.user.getProfileId(), true);
      }, Util.nonCriticalIoPool());
      this.userApiService = this.createUserApiService(this.authenticationService, pGameConfig);
      this.userPropertiesFuture = CompletableFuture.supplyAsync(() -> {
         try {
            return this.userApiService.fetchProperties();
         } catch (AuthenticationException authenticationexception) {
            LOGGER.error("Failed to fetch user properties", (Throwable)authenticationexception);
            return UserApiService.OFFLINE_PROPERTIES;
         }
      }, Util.nonCriticalIoPool());
      LOGGER.info("Setting user: {}", (Object)this.user.getName());
      LOGGER.debug("(Session ID is {})", (Object)this.user.getSessionId());
      this.demo = pGameConfig.game.demo;
      this.allowsMultiplayer = !pGameConfig.game.disableMultiplayer;
      this.allowsChat = !pGameConfig.game.disableChat;
      this.is64bit = checkIs64Bit();
      this.singleplayerServer = null;
      KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
      this.fixerUpper = DataFixers.getDataFixer();
      this.toast = new ToastComponent(this);
      this.gameThread = Thread.currentThread();
      this.options = new Options(this, this.gameDirectory);
      RenderSystem.setShaderGlintAlpha(this.options.glintStrength().get());
      this.running = true;
      this.tutorial = new Tutorial(this, this.options);
      this.hotbarManager = new HotbarManager(path, this.fixerUpper);
      LOGGER.info("Backend library: {}", (Object)RenderSystem.getBackendDescription());
      DisplayData displaydata;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         displaydata = new DisplayData(this.options.overrideWidth, this.options.overrideHeight, pGameConfig.display.fullscreenWidth, pGameConfig.display.fullscreenHeight, pGameConfig.display.isFullscreen);
      } else {
         displaydata = pGameConfig.display;
      }

      Util.timeSource = RenderSystem.initBackendSystem();
      this.virtualScreen = new VirtualScreen(this);
      this.window = this.virtualScreen.newWindow(displaydata, this.options.fullscreenVideoModeString, this.createTitle());
      this.setWindowActive(true);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

      try {
         this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't set icon", (Throwable)ioexception);
      }

      this.window.setFramerateLimit(this.options.framerateLimit().get());
      this.mouseHandler = new MouseHandler(this);
      this.mouseHandler.setup(this.window.getWindow());
      this.keyboardHandler = new KeyboardHandler(this);
      this.keyboardHandler.setup(this.window.getWindow());
      RenderSystem.initRenderer(this.options.glDebugVerbosity, false);
      this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
      this.mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.mainRenderTarget.clear(ON_OSX);
      this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
      this.resourcePackRepository.reload();
      this.options.loadSelectedResourcePacks(this.resourcePackRepository);
      this.languageManager = new LanguageManager(this.options.languageCode);
      this.resourceManager.registerReloadListener(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloadListener(this.textureManager);
      this.skinManager = new SkinManager(this.textureManager, file1.toPath().resolve("skins"), this.minecraftSessionService, this);
      this.levelSource = new LevelStorageSource(path.resolve("saves"), path.resolve("backups"), this.directoryValidator, this.fixerUpper);
      this.commandHistory = new CommandHistory(path);
      this.soundManager = new SoundManager(this.options);
      this.resourceManager.registerReloadListener(this.soundManager);
      this.splashManager = new SplashManager(this.user);
      this.resourceManager.registerReloadListener(this.splashManager);
      this.musicManager = new MusicManager(this);
      this.fontManager = new FontManager(this.textureManager);
      this.font = this.fontManager.createFont();
      this.fontFilterFishy = this.fontManager.createFontFilterFishy();
      this.resourceManager.registerReloadListener(this.fontManager);
      this.selectMainFont(this.isEnforceUnicode());
      this.resourceManager.registerReloadListener(new GrassColorReloadListener());
      this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
      this.window.setErrorSection("Startup");
      Vision.INSTANCE.setupClient();
      RenderSystem.setupDefaultState(0, 0, this.window.getWidth(), this.window.getHeight());
      this.window.setErrorSection("Post startup");
      this.blockColors = BlockColors.createDefault();
      this.itemColors = ItemColors.createDefault(this.blockColors);
      this.modelManager = new ModelManager(this.textureManager, this.blockColors, this.options.mipmapLevels().get());
      this.resourceManager.registerReloadListener(this.modelManager);
      this.entityModels = new EntityModelSet();
      this.resourceManager.registerReloadListener(this.entityModels);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(this.font, this.entityModels, this::getBlockRenderer, this::getItemRenderer, this::getEntityRenderDispatcher);
      this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
      BlockEntityWithoutLevelRenderer blockentitywithoutlevelrenderer = new BlockEntityWithoutLevelRenderer(this.blockEntityRenderDispatcher, this.entityModels);
      this.resourceManager.registerReloadListener(blockentitywithoutlevelrenderer);
      this.itemRenderer = new ItemRenderer(this, this.textureManager, this.modelManager, this.itemColors, blockentitywithoutlevelrenderer);
      this.resourceManager.registerReloadListener(this.itemRenderer);

      try {
         int i = Runtime.getRuntime().availableProcessors();
         int j = this.is64Bit() ? i : Math.min(i, 4);
         Tesselator.init();
         this.renderBuffers = new RenderBuffers(j);
      } catch (OutOfMemoryError outofmemoryerror) {
         TinyFileDialogs.tinyfd_messageBox("Minecraft", "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: https://aka.ms/Minecraft-Support", "ok", "error", true);
         throw new SilentInitException("Unable to allocate render buffers", outofmemoryerror);
      }

      this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
      this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), blockentitywithoutlevelrenderer, this.blockColors);
      this.resourceManager.registerReloadListener(this.blockRenderer);
      this.entityRenderDispatcher = new EntityRenderDispatcher(this, this.textureManager, this.itemRenderer, this.blockRenderer, this.font, this.options, this.entityModels);
      this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
      this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.resourceManager, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.gameRenderer.createReloadListener());
      this.levelRenderer = new LevelRenderer(this, this.entityRenderDispatcher, this.blockEntityRenderDispatcher, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.levelRenderer);
      this.createSearchTrees();
      this.resourceManager.registerReloadListener(this.searchRegistry);
      this.particleEngine = new ParticleEngine(this.level, this.textureManager);
      this.resourceManager.registerReloadListener(this.particleEngine);
      this.paintingTextures = new PaintingTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.paintingTextures);
      this.mobEffectTextures = new MobEffectTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.mobEffectTextures);
      this.guiSprites = new GuiSpriteManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.guiSprites);
      this.gpuWarnlistManager = new GpuWarnlistManager();
      this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
      this.resourceManager.registerReloadListener(this.regionalCompliancies);
      this.gui = new Gui(this, this.itemRenderer);
      this.debugRenderer = new DebugRenderer(this);
      RealmsClient realmsclient = RealmsClient.create(this);
      this.realmsDataFetcher = new RealmsDataFetcher(realmsclient);
      RenderSystem.setErrorCallback(this::onFullscreenError);

      if (this.mainRenderTarget.width == this.window.getWidth() && this.mainRenderTarget.height == this.window.getHeight()) {
         if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
            this.window.toggleFullScreen();
            this.options.fullscreen().set(this.window.isFullscreen());
         }
      } else {
         StringBuilder stringbuilder = new StringBuilder("Recovering from unsupported resolution (" + this.window.getWidth() + "x" + this.window.getHeight() + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).");
         if (GlDebug.isDebugEnabled()) {
            stringbuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", GlDebug.getLastOpenGlDebugMessages()));
         }

         this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
         TinyFileDialogs.tinyfd_messageBox("Minecraft", stringbuilder.toString(), "ok", "error", false);
      }

      this.window.updateVsync(this.options.enableVsync().get());
      this.window.updateRawMouseInput(this.options.rawMouseInput().get());
      this.window.setDefaultErrorCallback();
      this.resizeDisplay();
      this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
      this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
      this.profileKeyPairManager = ProfileKeyPairManager.create(this.userApiService, this.user, path);
      this.realms32BitWarningStatus = new Realms32BitWarningStatus(this);
      this.narrator = new GameNarrator(this);
      this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
      this.chatListener = new ChatListener(this);
      this.chatListener.setMessageDelay(this.options.chatDelay().get());
      this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
      LoadingOverlay.registerTextures(this);
      BaritoneAPI.getProvider().getPrimaryBaritone();
      this.setScreen(new GenericDirtMessageScreen(Component.translatable("gui.loadingMinecraft")));
      List<PackResources> list = this.resourcePackRepository.openAllSelected();
      this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, list);
      ReloadInstance reloadinstance = this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, list);
      GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      Minecraft.GameLoadCookie minecraft$gameloadcookie = new Minecraft.GameLoadCookie(realmsclient, pGameConfig.quickPlay);
      this.setOverlay(new LoadingOverlay(this, reloadinstance, (p_296164_) -> {
         Util.ifElse(p_296164_, (p_296162_) -> {
            this.rollbackResourcePacks(p_296162_, minecraft$gameloadcookie);
         }, () -> {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               this.selfTest();
            }

            this.reloadStateTracker.finishReload();
            this.onResourceLoadFinished(minecraft$gameloadcookie);
         });
      }, false));
      this.quickPlayLog = QuickPlayLog.of(pGameConfig.quickPlay.path());

      PostGameLoadCallback.EVENT.invoker().postGameLoad();
   }

   private void onResourceLoadFinished(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      if (!this.gameLoadFinished) {
         this.gameLoadFinished = true;
         this.onGameLoadFinished(pGameLoadCookie);
      }

   }

   private void onGameLoadFinished(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      Runnable runnable = this.buildInitialScreens(pGameLoadCookie);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
      GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
      runnable.run();
   }

   public boolean isGameLoadFinished() {
      return this.gameLoadFinished;
   }

   private Runnable buildInitialScreens(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      List<Function<Runnable, Screen>> list = new ArrayList<>();
      this.addInitialScreens(list);
      Runnable runnable = () -> {
         if (pGameLoadCookie != null && pGameLoadCookie.quickPlayData().isEnabled()) {
            QuickPlay.connect(this, pGameLoadCookie.quickPlayData(), pGameLoadCookie.realmsClient());
         } else {
            this.setScreen(new TitleScreen(true));
         }

      };

      for(Function<Runnable, Screen> function : Lists.reverse(list)) {
         Screen screen = function.apply(runnable);
         runnable = () -> {
            this.setScreen(screen);
         };
      }

      return runnable;
   }

   private void addInitialScreens(List<Function<Runnable, Screen>> pOutput) {
      if (this.options.onboardAccessibility) {
         pOutput.add((p_296165_) -> {
            return new AccessibilityOnboardingScreen(this.options, p_296165_);
         });
      }

      BanDetails bandetails = this.multiplayerBan();
      if (bandetails != null) {
         pOutput.add((p_296160_) -> {
            return BanNoticeScreens.create((p_296174_) -> {
               if (p_296174_) {
                  Util.getPlatform().openUri("https://aka.ms/mcjavamoderation");
               }

               p_296160_.run();
            }, bandetails);
         });
      }

      ProfileResult profileresult = this.profileFuture.join();
      if (profileresult != null) {
         GameProfile gameprofile = profileresult.profile();
         Set<ProfileActionType> set = profileresult.actions();
         if (set.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
            pOutput.add((p_296154_) -> {
               return BanNoticeScreens.createNameBan(gameprofile.getName(), p_296154_);
            });
         }

         if (set.contains(ProfileActionType.USING_BANNED_SKIN)) {
            pOutput.add(BanNoticeScreens::createSkinBan);
         }
      }

   }

   private static boolean countryEqualsISO3(Object p_210783_) {
      try {
         return Locale.getDefault().getISO3Country().equals(p_210783_);
      } catch (MissingResourceException missingresourceexception) {
         return false;
      }
   }

   public void updateTitle() {
      this.window.setTitle(this.createTitle());
   }

   private String createTitle() {
      StringBuilder stringbuilder = new StringBuilder(Vision.INSTANCE.CLIENT_NAME + " - " + Vision.INSTANCE.CLIENT_VERSION + " | " + "Minecraft");
      if (checkModStatus().shouldReportAsModified()) {
         stringbuilder.append("*");
      }

      stringbuilder.append(" ");
      stringbuilder.append(SharedConstants.getCurrentVersion().getName());
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null && clientpacketlistener.getConnection().isConnected()) {
         stringbuilder.append(" - ");
         ServerData serverdata = this.getCurrentServer();
         if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
            stringbuilder.append(I18n.get("title.singleplayer"));
         } else if (serverdata != null && serverdata.isRealm()) {
            stringbuilder.append(I18n.get("title.multiplayer.realms"));
         } else if (this.singleplayerServer == null && (serverdata == null || !serverdata.isLan())) {
            stringbuilder.append(I18n.get("title.multiplayer.other"));
         } else {
            stringbuilder.append(I18n.get("title.multiplayer.lan"));
         }
      }

      return stringbuilder.toString();
   }

   private UserApiService createUserApiService(YggdrasilAuthenticationService pAuthenticationService, GameConfig pGameConfig) {
      return pAuthenticationService.createUserApiService(pGameConfig.user.user.getAccessToken());
   }

   public static ModCheck checkModStatus() {
      return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
   }

   private void rollbackResourcePacks(Throwable pThrowable, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      if (this.resourcePackRepository.getSelectedIds().size() > 1) {
         this.clearResourcePacksOnError(pThrowable, (Component)null, pGameLoadCookie);
      } else {
         Util.throwAsRuntime(pThrowable);
      }

   }

   public void clearResourcePacksOnError(Throwable pThrowable, @Nullable Component pErrorMessage, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", pThrowable);
      this.reloadStateTracker.startRecovery(pThrowable);
      this.downloadedPackSource.onRecovery();
      this.resourcePackRepository.setSelected(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.save();
      this.reloadResourcePacks(true, pGameLoadCookie).thenRun(() -> {
         this.addResourcePackLoadFailToast(pErrorMessage);
      });
   }

   private void abortResourcePackRecovery() {
      this.setOverlay((Overlay)null);
      if (this.level != null) {
         this.level.disconnect();
         this.disconnect();
      }

      this.setScreen(new TitleScreen());
      this.addResourcePackLoadFailToast((Component)null);
   }

   private void addResourcePackLoadFailToast(@Nullable Component pMessage) {
      ToastComponent toastcomponent = this.getToasts();
      SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), pMessage);
   }

   public void run() {
      this.gameThread = Thread.currentThread();
      ClientLifecycleEvents.CLIENT_STARTED.invoker().onClientStarted(this);
      if (Runtime.getRuntime().availableProcessors() > 4) {
         this.gameThread.setPriority(10);
      }

      try {
         boolean flag = false;

         while(this.running) {
            this.handleDelayedCrash();

            try {
               SingleTickProfiler singletickprofiler = SingleTickProfiler.createTickProfiler("Renderer");
               boolean flag1 = this.getDebugOverlay().showProfilerChart();
               this.profiler = this.constructProfiler(flag1, singletickprofiler);
               this.profiler.startTick();
               this.metricsRecorder.startTick();
               this.runTick(!flag);
               this.metricsRecorder.endTick();
               this.profiler.endTick();
               this.finishProfilers(flag1, singletickprofiler);
            } catch (OutOfMemoryError outofmemoryerror) {
               if (flag) {
                  throw outofmemoryerror;
               }

               this.emergencySave();
               this.setScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", (Throwable)outofmemoryerror);
               flag = true;
            }
         }
      } catch (ReportedException reportedexception) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", (Throwable)reportedexception);
         this.emergencySaveAndCrash(reportedexception.getReport());
      } catch (Throwable throwable) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", throwable);
         this.emergencySaveAndCrash(new CrashReport("Unexpected error", throwable));
      }

   }

   void selectMainFont(boolean pForced) {
      this.fontManager.setRenames(pForced ? ImmutableMap.of(DEFAULT_FONT, UNIFORM_FONT) : ImmutableMap.of());
   }

   private void createSearchTrees() {
      this.searchRegistry.register(SearchRegistry.CREATIVE_NAMES, (p_231451_) -> new FullTextSearchTree<>((p_210797_) -> p_210797_.getTooltipLines(null, TooltipFlag.Default.NORMAL.asCreative()).stream().map((p_231455_) -> ChatFormatting.stripFormatting(p_231455_.getString()).trim()).filter((p_231449_) -> !p_231449_.isEmpty()), (p_91317_) -> Stream.of(BuiltInRegistries.ITEM.getKey(p_91317_.getItem())), p_231451_));
      this.searchRegistry.register(SearchRegistry.CREATIVE_TAGS, (p_231430_) -> new IdSearchTree<>((p_231353_) -> p_231353_.getTags().map(TagKey::location), p_231430_));
      this.searchRegistry.register(SearchRegistry.RECIPE_COLLECTIONS, (p_301514_) -> new FullTextSearchTree<>((p_301513_) -> p_301513_.getRecipes().stream().flatMap((p_301519_) -> p_301519_.value().getResultItem(p_301513_.registryAccess()).getTooltipLines((Player)null, TooltipFlag.Default.NORMAL).stream()).map((p_301515_) -> ChatFormatting.stripFormatting(p_301515_.getString()).trim()).filter((p_301516_) -> !p_301516_.isEmpty()), (p_301517_) -> p_301517_.getRecipes().stream().map((p_301512_) -> BuiltInRegistries.ITEM.getKey(p_301512_.value().getResultItem(p_301517_.registryAccess()).getItem())), p_301514_));
      CreativeModeTabs.searchTab().setSearchTreeBuilder((p_255439_) -> {
         this.populateSearchTree(SearchRegistry.CREATIVE_NAMES, p_255439_);
         this.populateSearchTree(SearchRegistry.CREATIVE_TAGS, p_255439_);
      });
   }

   private void onFullscreenError(int p_91114_, long p_91115_) {
      this.options.enableVsync().set(false);
      this.options.save();
   }

   private static boolean checkIs64Bit() {
      String[] astring = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};

      for(String s : astring) {
         String s1 = System.getProperty(s);
         if (s1 != null && s1.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public RenderTarget getMainRenderTarget() {
      return this.mainRenderTarget;
   }

   public String getLaunchedVersion() {
      return this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void delayCrash(CrashReport pReport) {
      this.delayedCrash = () -> {
         return this.fillReport(pReport);
      };
   }

   public void delayCrashRaw(CrashReport pReport) {
      this.delayedCrash = () -> {
         return pReport;
      };
   }

   private void handleDelayedCrash() {
      if (this.delayedCrash != null) {
         crash(this, this.gameDirectory, this.delayedCrash.get());
      }

   }

   public void emergencySaveAndCrash(CrashReport pCrashReport) {
      CrashReport crashreport = this.fillReport(pCrashReport);
      this.emergencySave();
      crash(this, this.gameDirectory, crashreport);
   }

   public static void crash(@Nullable Minecraft pMinecraft, File pGameDirectory, CrashReport pCrashReport) {
      File file1 = new File(pGameDirectory, "crash-reports");
      File file2 = new File(file1, "crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Bootstrap.realStdoutPrintln(pCrashReport.getFriendlyReport());
      if (pMinecraft != null) {
         pMinecraft.soundManager.emergencyShutdown();
      }

      if (pCrashReport.getSaveFile() != null) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + pCrashReport.getSaveFile());
         System.exit(-1);
      } else if (pCrashReport.saveToFile(file2)) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
         System.exit(-1);
      } else {
         Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         System.exit(-2);
      }

   }

   public boolean isEnforceUnicode() {
      return this.options.forceUnicodeFont().get();
   }

   public CompletableFuture<Void> reloadResourcePacks() {
      return this.reloadResourcePacks(false, (Minecraft.GameLoadCookie)null);
   }

   private CompletableFuture<Void> reloadResourcePacks(boolean pError, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
      if (this.pendingReload != null) {
         return this.pendingReload;
      } else {
         CompletableFuture<Void> completablefuture = new CompletableFuture<>();
         if (!pError && this.overlay instanceof LoadingOverlay) {
            this.pendingReload = completablefuture;
            return completablefuture;
         } else {
            this.resourcePackRepository.reload();
            List<PackResources> list = this.resourcePackRepository.openAllSelected();
            if (!pError) {
               this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, list);
            }

            this.setOverlay(new LoadingOverlay(this, this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, list), (p_296158_) -> {
               Util.ifElse(p_296158_, (p_308166_) -> {
                  if (pError) {
                     this.downloadedPackSource.onRecoveryFailure();
                     this.abortResourcePackRecovery();
                  } else {
                     this.rollbackResourcePacks(p_308166_, pGameLoadCookie);
                  }

               }, () -> {
                  this.levelRenderer.allChanged();
                  this.reloadStateTracker.finishReload();
                  this.downloadedPackSource.onReloadSuccess();
                  completablefuture.complete((Void)null);
                  this.onResourceLoadFinished(pGameLoadCookie);
               });
            }, !pError));
            return completablefuture;
         }
      }
   }

   private void selfTest() {
      boolean flag = false;
      BlockModelShaper blockmodelshaper = this.getBlockRenderer().getBlockModelShaper();
      BakedModel bakedmodel = blockmodelshaper.getModelManager().getMissingModel();

      for(Block block : BuiltInRegistries.BLOCK) {
         for(BlockState blockstate : block.getStateDefinition().getPossibleStates()) {
            if (blockstate.getRenderShape() == RenderShape.MODEL) {
               BakedModel bakedmodel1 = blockmodelshaper.getBlockModel(blockstate);
               if (bakedmodel1 == bakedmodel) {
                  LOGGER.debug("Missing model for: {}", (Object)blockstate);
                  flag = true;
               }
            }
         }
      }

      TextureAtlasSprite textureatlassprite1 = bakedmodel.getParticleIcon();

      for(Block block1 : BuiltInRegistries.BLOCK) {
         for(BlockState blockstate1 : block1.getStateDefinition().getPossibleStates()) {
            TextureAtlasSprite textureatlassprite = blockmodelshaper.getParticleIcon(blockstate1);
            if (!blockstate1.isAir() && textureatlassprite == textureatlassprite1) {
               LOGGER.debug("Missing particle icon for: {}", (Object)blockstate1);
            }
         }
      }

      for(Item item : BuiltInRegistries.ITEM) {
         ItemStack itemstack = item.getDefaultInstance();
         String s = itemstack.getDescriptionId();
         String s1 = Component.translatable(s).getString();
         if (s1.toLowerCase(Locale.ROOT).equals(item.getDescriptionId())) {
            LOGGER.debug("Missing translation for: {} {} {}", itemstack, s, item);
         }
      }

      flag |= MenuScreens.selfTest();
      flag |= EntityRenderers.validateRegistrations();
      if (flag) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorageSource getLevelSource() {
      return this.levelSource;
   }

   private void openChatScreen(String pDefaultText) {
      Minecraft.ChatStatus minecraft$chatstatus = this.getChatStatus();
      if (!minecraft$chatstatus.isChatAllowed(this.isLocalServer())) {
         if (this.gui.isShowingChatDisabledByPlayer()) {
            this.gui.setChatDisabledByPlayerShown(false);
            this.setScreen(new ConfirmLinkScreen((p_243338_) -> {
               if (p_243338_) {
                  Util.getPlatform().openUri("https://aka.ms/JavaAccountSettings");
               }

               this.setScreen((Screen)null);
            }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, "https://aka.ms/JavaAccountSettings", true));
         } else {
            Component component = minecraft$chatstatus.getMessage();
            this.gui.setOverlayMessage(component, false);
            this.narrator.sayNow(component);
            this.gui.setChatDisabledByPlayerShown(minecraft$chatstatus == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
         }
      } else {
         this.setScreen(new ChatScreen(pDefaultText));
      }

   }

   public void setScreen(@Nullable Screen pGuiScreen) {
      if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
         LOGGER.error("setScreen called from non-game thread");
      }

      if (this.screen != null) {
         this.screen.removed();
      }

      if (pGuiScreen == null && this.clientLevelTeardownInProgress) {
         throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
      } else {
         if (pGuiScreen == null && this.level == null) {
            pGuiScreen = new TitleScreen();
         } else if (pGuiScreen == null && this.player.isDeadOrDying()) {
            if (this.player.shouldShowDeathScreen()) {
               pGuiScreen = new DeathScreen((Component)null, this.level.getLevelData().isHardcore());
            } else {
               this.player.respawn();
            }
         }

         MuiModApi.dispatchOnScreenChange(screen, pGuiScreen);
         this.screen = pGuiScreen;
         if (this.screen != null) {
            this.screen.added();
         }

         BufferUploader.reset();
         if (pGuiScreen != null) {
            this.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            pGuiScreen.init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            this.noRender = false;
         } else {
            this.soundManager.resume();
            this.mouseHandler.grabMouse();
         }

         this.updateTitle();
      }
   }

   public void setOverlay(@Nullable Overlay pLoadingGui) {
      this.overlay = pLoadingGui;
   }

   public void destroy() {
      try {
         LOGGER.info("Stopping!");
         ClientLifecycleEvents.CLIENT_STOPPING.invoker().onClientStopping(this);

         try {
            this.narrator.destroy();
         } catch (Throwable throwable1) {
         }

         try {
            if (this.level != null) {
               this.level.disconnect();
            }

            this.disconnect();
         } catch (Throwable throwable) {
         }

         if (this.screen != null) {
            this.screen.removed();
         }

         this.close();
      } finally {
         Util.timeSource = System::nanoTime;
         if (this.delayedCrash == null) {
            System.exit(0);
         }

      }

   }

   public void close() {
      if (this.currentFrameProfile != null) {
         this.currentFrameProfile.cancel();
      }

      try {
         this.telemetryManager.close();
         this.regionalCompliancies.close();
         this.modelManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.levelRenderer.close();
         this.soundManager.destroy();
         this.particleEngine.close();
         this.mobEffectTextures.close();
         this.paintingTextures.close();
         this.guiSprites.close();
         this.textureManager.close();
         this.resourceManager.close();
         UIManager.destroy();
         Util.shutdownExecutors();
      } catch (Throwable throwable) {
         LOGGER.error("Shutdown failure!", throwable);
         throw throwable;
      } finally {
         this.virtualScreen.close();
         this.window.close();
      }

   }

   private void runTick(boolean pRenderLevel) {
      EventManager.call(new EventTick());
      this.window.setErrorSection("Pre render");
      long i = Util.getNanos();
      if (this.window.shouldClose()) {
         this.stop();
      }

      if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
         CompletableFuture<Void> completablefuture = this.pendingReload;
         this.pendingReload = null;
         this.reloadResourcePacks().thenRun(() -> {
            completablefuture.complete((Void)null);
         });
      }

      Runnable runnable;
      while((runnable = this.progressTasks.poll()) != null) {
         runnable.run();
      }

      if (pRenderLevel) {
         int j = this.timer.advanceTime(Util.getMillis());
         this.profiler.push("scheduledExecutables");
         this.runAllTasks();
         this.profiler.pop();
         this.profiler.push("tick");

         for(int k = 0; k < Math.min(10, j); ++k) {
            this.profiler.incrementCounter("clientTick");
            this.tick();
         }

         this.profiler.pop();
      }

      this.mouseHandler.turnPlayer();
      this.window.setErrorSection("Render");
      this.profiler.push("sound");
      this.soundManager.updateSource(this.gameRenderer.getMainCamera());
      this.profiler.pop();
      this.profiler.push("render");
      long j1 = Util.getNanos();
      boolean flag;
      if (!this.getDebugOverlay().showDebugScreen() && !this.metricsRecorder.isRecording()) {
         flag = false;
         this.gpuUtilization = 0.0D;
      } else {
         flag = this.currentFrameProfile == null || this.currentFrameProfile.isDone();
         if (flag) {
            TimerQuery.getInstance().ifPresent(TimerQuery::beginProfile);
         }
      }

      RenderSystem.clear(16640, ON_OSX);
      this.mainRenderTarget.bindWrite(true);
      FogRenderer.setupNoFog();
      this.profiler.push("display");
      RenderSystem.enableCull();
      this.profiler.pop();
      if (!this.noRender) {
         this.profiler.popPush("gameRenderer");
         ModernUIFabricClient.START_RENDER_TICK.invoker().run();
         this.gameRenderer.render(this.pause ? this.pausePartialTick : this.timer.partialTick, i, pRenderLevel);
         ModernUIFabricClient.END_RENDER_TICK.invoker().run();
         this.profiler.pop();
      }

      if (this.fpsPieResults != null) {
         this.profiler.push("fpsPie");
         GuiGraphics guigraphics = new GuiGraphics(this, this.renderBuffers.bufferSource());
         this.renderFpsMeter(guigraphics, this.fpsPieResults);
         guigraphics.flush();
         this.profiler.pop();
      }

      this.profiler.push("blit");
      this.mainRenderTarget.unbindWrite();
      this.mainRenderTarget.blitToScreen(this.window.getWidth(), this.window.getHeight());
      this.frameTimeNs = Util.getNanos() - j1;
      if (flag) {
         TimerQuery.getInstance().ifPresent((p_231363_) -> {
            this.currentFrameProfile = p_231363_.endProfile();
         });
      }

      this.profiler.popPush("updateDisplay");
      this.window.updateDisplay();
      int k1 = this.getFramerateLimit();
      if (k1 < 260) {
         RenderSystem.limitDisplayFPS(k1);
      }

      this.profiler.popPush("yield");
      Thread.yield();
      this.profiler.pop();
      this.window.setErrorSection("Post render");
      ++this.frames;
      boolean flag1 = this.hasSingleplayerServer() && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen()) && !this.singleplayerServer.isPublished();
      if (this.pause != flag1) {
         if (flag1) {
            this.pausePartialTick = this.timer.partialTick;
         } else {
            this.timer.partialTick = this.pausePartialTick;
         }

         this.pause = flag1;
      }

      long l = Util.getNanos();
      long i1 = l - this.lastNanoTime;
      if (flag) {
         this.savedCpuDuration = i1;
      }

      this.getDebugOverlay().logFrameDuration(i1);
      this.lastNanoTime = l;
      this.profiler.push("fpsUpdate");
      if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
         this.gpuUtilization = (double)this.currentFrameProfile.get() * 100.0D / (double)this.savedCpuDuration;
      }

      while(Util.getMillis() >= this.lastTime + 1000L) {
         String s;
         if (this.gpuUtilization > 0.0D) {
            s = " GPU: " + (this.gpuUtilization > 100.0D ? ChatFormatting.RED + "100%" : Math.round(this.gpuUtilization) + "%");
         } else {
            s = "";
         }

         fps = this.frames;
         this.fpsString = String.format(Locale.ROOT, "%d fps T: %s%s%s%s B: %d%s", fps, k1 == 260 ? "inf" : k1, this.options.enableVsync().get() ? " vsync" : "", this.options.graphicsMode().get(), this.options.cloudStatus().get() == CloudStatus.OFF ? "" : (this.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius().get(), s);
         this.lastTime += 1000L;
         this.frames = 0;
      }

      this.profiler.pop();
   }

   private ProfilerFiller constructProfiler(boolean pRenderFpsPie, @Nullable SingleTickProfiler pSingleTickProfiler) {
      if (!pRenderFpsPie) {
         this.fpsPieProfiler.disable();
         if (!this.metricsRecorder.isRecording() && pSingleTickProfiler == null) {
            return InactiveProfiler.INSTANCE;
         }
      }

      ProfilerFiller profilerfiller;
      if (pRenderFpsPie) {
         if (!this.fpsPieProfiler.isEnabled()) {
            this.fpsPieRenderTicks = 0;
            this.fpsPieProfiler.enable();
         }

         ++this.fpsPieRenderTicks;
         profilerfiller = this.fpsPieProfiler.getFiller();
      } else {
         profilerfiller = InactiveProfiler.INSTANCE;
      }

      if (this.metricsRecorder.isRecording()) {
         profilerfiller = ProfilerFiller.tee(profilerfiller, this.metricsRecorder.getProfiler());
      }

      return SingleTickProfiler.decorateFiller(profilerfiller, pSingleTickProfiler);
   }

   private void finishProfilers(boolean pRenderFpsPie, @Nullable SingleTickProfiler pProfiler) {
      if (pProfiler != null) {
         pProfiler.endTick();
      }

      if (pRenderFpsPie) {
         this.fpsPieResults = this.fpsPieProfiler.getResults();
      } else {
         this.fpsPieResults = null;
      }

      this.profiler = this.fpsPieProfiler.getFiller();
   }

   public void resizeDisplay() {
      int i = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
      this.window.setGuiScale((double)i);
      if (this.screen != null) {
         this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
      }

      RenderTarget rendertarget = this.getMainRenderTarget();
      rendertarget.resize(this.window.getWidth(), this.window.getHeight(), ON_OSX);
      this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
      this.mouseHandler.setIgnoreFirstMove();
   }

   public void cursorEntered() {
      this.mouseHandler.cursorEntered();
   }

   public int getFps() {
      return fps;
   }

   public long getFrameTimeNs() {
      return this.frameTimeNs;
   }

   private int getFramerateLimit() {
      if ((BlurHandler.sFramerateInactive != 0 ||
              BlurHandler.sFramerateMinimized != 0) &&
              !isWindowActive()) {
         if (BlurHandler.sFramerateMinimized != 0 &&
                 BlurHandler.sFramerateMinimized < BlurHandler.sFramerateInactive &&
                 GLFW.glfwGetWindowAttrib(window.getWindow(), GLFW.GLFW_ICONIFIED) != 0) {
            return (Math.min(
                    BlurHandler.sFramerateMinimized,
                    window.getFramerateLimit()
            ));
         } else if (BlurHandler.sFramerateInactive != 0) {
            return (Math.min(
                    BlurHandler.sFramerateInactive,
                    window.getFramerateLimit()
            ));
         }
      }
      return this.level != null || this.screen == null && this.overlay == null ? this.window.getFramerateLimit() : 60;
   }

   private void emergencySave() {
      try {
         MemoryReserve.release();
         this.levelRenderer.clear();
      } catch (Throwable throwable1) {
      }

      try {
         System.gc();
         if (this.isLocalServer && this.singleplayerServer != null) {
            this.singleplayerServer.halt(true);
         }

         this.disconnect(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
      } catch (Throwable throwable) {
      }

      System.gc();
   }

   public boolean debugClientMetricsStart(Consumer<Component> pLogger) {
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsStop();
         return false;
      } else {
         Consumer<ProfileResults> consumer = (p_231435_) -> {
            if (p_231435_ != EmptyProfileResults.EMPTY) {
               int i = p_231435_.getTickDuration();
               double d0 = (double)p_231435_.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
               this.execute(() -> {
                  pLogger.accept(Component.translatable("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d0), i, String.format(Locale.ROOT, "%.2f", (double)i / d0)));
               });
            }
         };
         Consumer<Path> consumer1 = (p_231438_) -> {
            Component component = Component.literal(p_231438_.toString()).withStyle(ChatFormatting.UNDERLINE).withStyle((p_231387_) -> {
               return p_231387_.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, p_231438_.toFile().getParent()));
            });
            this.execute(() -> {
               pLogger.accept(Component.translatable("debug.profiling.stop", component));
            });
         };
         SystemReport systemreport = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
         Consumer<List<Path>> consumer2 = (p_231349_) -> {
            Path path = this.archiveProfilingReport(systemreport, p_231349_);
            consumer1.accept(path);
         };
         Consumer<Path> consumer3;
         if (this.singleplayerServer == null) {
            consumer3 = (p_231404_) -> {
               consumer2.accept(ImmutableList.of(p_231404_));
            };
         } else {
            this.singleplayerServer.fillSystemReport(systemreport);
            CompletableFuture<Path> completablefuture = new CompletableFuture<>();
            CompletableFuture<Path> completablefuture1 = new CompletableFuture<>();
            CompletableFuture.allOf(completablefuture, completablefuture1).thenRunAsync(() -> {
               consumer2.accept(ImmutableList.of(completablefuture.join(), completablefuture1.join()));
            }, Util.ioPool());
            this.singleplayerServer.startRecordingMetrics((p_231351_) -> {
            }, completablefuture1::complete);
            consumer3 = completablefuture::complete;
         }

         this.metricsRecorder = ActiveMetricsRecorder.createStarted(new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer), Util.timeSource, Util.ioPool(), new MetricsPersister("client"), (p_231401_) -> {
            this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
            consumer.accept(p_231401_);
         }, consumer3);
         return true;
      }
   }

   private void debugClientMetricsStop() {
      this.metricsRecorder.end();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.finishRecordingMetrics();
      }

   }

   private void debugClientMetricsCancel() {
      this.metricsRecorder.cancel();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.cancelRecordingMetrics();
      }

   }

   private Path archiveProfilingReport(SystemReport pReport, List<Path> pPaths) {
      String s;
      if (this.isLocalServer()) {
         s = this.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData serverdata = this.getCurrentServer();
         s = serverdata != null ? serverdata.name : "unknown";
      }

      Path path;
      try {
         String s2 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), s, SharedConstants.getCurrentVersion().getId());
         String s1 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, s2, ".zip");
         path = MetricsPersister.PROFILING_RESULTS_DIR.resolve(s1);
      } catch (IOException ioexception1) {
         throw new UncheckedIOException(ioexception1);
      }

      try (FileZipper filezipper = new FileZipper(path)) {
         filezipper.add(Paths.get("system.txt"), pReport.toLineSeparatedString());
         filezipper.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
         pPaths.forEach(filezipper::add);
      } finally {
         for(Path path1 : pPaths) {
            try {
               FileUtils.forceDelete(path1.toFile());
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to delete temporary profiling result {}", path1, ioexception);
            }
         }

      }

      return path;
   }

   public void debugFpsMeterKeyPress(int pKeyCount) {
      if (this.fpsPieResults != null) {
         List<ResultField> list = this.fpsPieResults.getTimes(this.debugPath);
         if (!list.isEmpty()) {
            ResultField resultfield = list.remove(0);
            if (pKeyCount == 0) {
               if (!resultfield.name.isEmpty()) {
                  int i = this.debugPath.lastIndexOf(30);
                  if (i >= 0) {
                     this.debugPath = this.debugPath.substring(0, i);
                  }
               }
            } else {
               --pKeyCount;
               if (pKeyCount < list.size() && !"unspecified".equals((list.get(pKeyCount)).name)) {
                  if (!this.debugPath.isEmpty()) {
                     this.debugPath = this.debugPath + "\u001e";
                  }

                  this.debugPath = this.debugPath + (list.get(pKeyCount)).name;
               }
            }

         }
      }
   }

   private void renderFpsMeter(GuiGraphics pGuiGraphics, ProfileResults pProfileResults) {
      List<ResultField> list = pProfileResults.getTimes(this.debugPath);
      ResultField resultfield = list.remove(0);
      RenderSystem.clear(256, ON_OSX);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)this.window.getWidth(), (float)this.window.getHeight(), 0.0F, 1000.0F, 3000.0F);
      RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      posestack.setIdentity();
      posestack.translate(0.0F, 0.0F, -2000.0F);
      RenderSystem.applyModelViewMatrix();
      RenderSystem.lineWidth(1.0F);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      int i = 160;
      int j = this.window.getWidth() - 160 - 10;
      int k = this.window.getHeight() - 320;
      RenderSystem.enableBlend();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex((double)((float)j - 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      bufferbuilder.vertex((double)((float)j - 176.0F), (double)(k + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      bufferbuilder.vertex((double)((float)j + 176.0F), (double)(k + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      bufferbuilder.vertex((double)((float)j + 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      tesselator.end();
      RenderSystem.disableBlend();
      double d0 = 0.0D;

      for(ResultField resultfield1 : list) {
         int l = Mth.floor(resultfield1.percentage / 4.0D) + 1;
         bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         int i1 = resultfield1.getColor();
         int j1 = i1 >> 16 & 255;
         int k1 = i1 >> 8 & 255;
         int l1 = i1 & 255;
         bufferbuilder.vertex((double)j, (double)k, 0.0D).color(j1, k1, l1, 255).endVertex();

         for(int i2 = l; i2 >= 0; --i2) {
            float f = (float)((d0 + resultfield1.percentage * (double)i2 / (double)l) * (double)((float)Math.PI * 2F) / 100.0D);
            float f1 = Mth.sin(f) * 160.0F;
            float f2 = Mth.cos(f) * 160.0F * 0.5F;
            bufferbuilder.vertex((double)((float)j + f1), (double)((float)k - f2), 0.0D).color(j1, k1, l1, 255).endVertex();
         }

         tesselator.end();
         bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

         for(int l2 = l; l2 >= 0; --l2) {
            float f3 = (float)((d0 + resultfield1.percentage * (double)l2 / (double)l) * (double)((float)Math.PI * 2F) / 100.0D);
            float f4 = Mth.sin(f3) * 160.0F;
            float f5 = Mth.cos(f3) * 160.0F * 0.5F;
            if (!(f5 > 0.0F)) {
               bufferbuilder.vertex((double)((float)j + f4), (double)((float)k - f5), 0.0D).color(j1 >> 1, k1 >> 1, l1 >> 1, 255).endVertex();
               bufferbuilder.vertex((double)((float)j + f4), (double)((float)k - f5 + 10.0F), 0.0D).color(j1 >> 1, k1 >> 1, l1 >> 1, 255).endVertex();
            }
         }

         tesselator.end();
         d0 += resultfield1.percentage;
      }

      DecimalFormat decimalformat = new DecimalFormat("##0.00");
      decimalformat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      String s = ProfileResults.demanglePath(resultfield.name);
      String s1 = "";
      if (!"unspecified".equals(s)) {
         s1 = s1 + "[0] ";
      }

      if (s.isEmpty()) {
         s1 = s1 + "ROOT ";
      } else {
         s1 = s1 + s + " ";
      }

      int k2 = 16777215;
      pGuiGraphics.drawString(this.font, s1, j - 160, k - 80 - 16, 16777215);
      s1 = decimalformat.format(resultfield.globalPercentage) + "%";
      pGuiGraphics.drawString(this.font, s1, j + 160 - this.font.width(s1), k - 80 - 16, 16777215);

      for(int j2 = 0; j2 < list.size(); ++j2) {
         ResultField resultfield2 = list.get(j2);
         StringBuilder stringbuilder = new StringBuilder();
         if ("unspecified".equals(resultfield2.name)) {
            stringbuilder.append("[?] ");
         } else {
            stringbuilder.append("[").append(j2 + 1).append("] ");
         }

         String s2 = stringbuilder.append(resultfield2.name).toString();
         pGuiGraphics.drawString(this.font, s2, j - 160, k + 80 + j2 * 8 + 20, resultfield2.getColor());
         s2 = decimalformat.format(resultfield2.percentage) + "%";
         pGuiGraphics.drawString(this.font, s2, j + 160 - 50 - this.font.width(s2), k + 80 + j2 * 8 + 20, resultfield2.getColor());
         s2 = decimalformat.format(resultfield2.globalPercentage) + "%";
         pGuiGraphics.drawString(this.font, s2, j + 160 - this.font.width(s2), k + 80 + j2 * 8 + 20, resultfield2.getColor());
      }

      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   public void stop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void pauseGame(boolean pPauseOnly) {
      if (this.screen == null) {
         boolean flag = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
         if (flag) {
            this.setScreen(new PauseScreen(!pPauseOnly));
            this.soundManager.pause();
         } else {
            this.setScreen(new PauseScreen(true));
         }

      }
   }

   private void continueAttack(boolean pLeftClick) {
      if (!pLeftClick) {
         this.missTime = 0;
      }

      if (this.missTime <= 0 && !this.player.isUsingItem() && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_7_6)) {
         if (pLeftClick && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
            BlockPos blockpos = blockhitresult.getBlockPos();
            if (!this.level.getBlockState(blockpos).isAir()) {
               Direction direction = blockhitresult.getDirection();
               if (this.gameMode.continueDestroyBlock(blockpos, direction)) {
                  this.particleEngine.crack(blockpos, direction);
                  this.player.swing(InteractionHand.MAIN_HAND);
               }
            }

         } else {
            this.gameMode.stopDestroyBlock();
         }
      }
   }

   private boolean startAttack() {
      if (this.missTime > 0) {
         return false;
      } else if (this.hitResult == null) {
         LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
         if (this.gameMode.hasMissTime()) {
            this.missTime = 10;
         }

         return false;
      } else if (this.player.isHandsBusy()) {
         return false;
      } else {
         ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
            return false;
         } else {
            boolean flag = false;
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
               this.player.swing(InteractionHand.MAIN_HAND);
            }
            switch (this.hitResult.getType()) {
               case ENTITY:
                  this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
                  break;
               case BLOCK:
                  BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                  BlockPos blockpos = blockhitresult.getBlockPos();
                  if (!this.level.getBlockState(blockpos).isAir()) {
                     this.gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
                     if (this.level.getBlockState(blockpos).isAir()) {
                        flag = true;
                     }
                     break;
                  }
               case MISS:
                  if (this.gameMode.hasMissTime()) {
                     this.missTime = 10;
                  }

                  this.player.resetAttackStrengthTicker();
            }
            if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_8)){
               this.player.swing(InteractionHand.MAIN_HAND);
            }
            return flag;
         }
      }
   }

   private void startUseItem() {
      if (!this.gameMode.isDestroying()) {
         this.rightClickDelay = 4;
         if (!this.player.isHandsBusy()) {
            if (this.hitResult == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            for(InteractionHand interactionhand : InteractionHand.values()) {
               ItemStack itemstack = this.player.getItemInHand(interactionhand);
               if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                  return;
               }

               if (this.hitResult != null) {
                  switch (this.hitResult.getType()) {
                     case ENTITY:
                        EntityHitResult entityhitresult = (EntityHitResult)this.hitResult;
                        Entity entity = entityhitresult.getEntity();
                        if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                           return;
                        }

                        InteractionResult interactionresult = this.gameMode.interactAt(this.player, entity, entityhitresult, interactionhand);
                        if (!interactionresult.consumesAction()) {
                           interactionresult = this.gameMode.interact(this.player, entity, interactionhand);
                        }

                        if (interactionresult.consumesAction()) {
                           if (interactionresult.shouldSwing() && ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15)) {
                              this.player.swing(interactionhand);
                           }

                           return;
                        }
                        break;
                     case BLOCK:
                        BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                        int i = itemstack.getCount();
                        InteractionResult interactionresult1 = this.gameMode.useItemOn(this.player, interactionhand, blockhitresult);
                        if (interactionresult1.consumesAction()) {
                           if (interactionresult1.shouldSwing()) {
                              this.player.swing(interactionhand);
                              if (!itemstack.isEmpty() && (itemstack.getCount() != i || this.gameMode.hasInfiniteItems())) {
                                 this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                              }
                           }

                           return;
                        }

                        if (interactionresult1 == InteractionResult.FAIL) {
                           return;
                        }
                  }
               }

               if (!itemstack.isEmpty()) {
                  InteractionResult interactionresult2 = this.gameMode.useItem(this.player, interactionhand);
                  if (interactionresult2.consumesAction()) {
                     if (interactionresult2.shouldSwing() && ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15)) {
                        this.player.swing(interactionhand);
                     }

                     this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                     return;
                  }
               }
            }

         }
      }
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }
   private Screen passEvents(Minecraft instance) {
      // allow user input is only the primary baritone
      if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && player != null) {
         return null;
      }
      return instance.screen;
   }
   private float handleMissTime(){
      //MoveCooldownIncrement
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return 0;
      } else {
         return missTime;
      }
   }

   public void tick() {
      EntityCullingMod.INSTANCE.clientTick();
      ++this.clientTickCount;
      if (this.level != null && !this.pause) {
         this.level.tickRateManager().tick();
      }

      if (this.rightClickDelay > 0) {
         --this.rightClickDelay;
      }

      this.profiler.push("gui");
      this.chatListener.tick();
      this.gui.tick(this.pause);
      this.profiler.pop();
      this.gameRenderer.pick(1.0F);
      this.tutorial.onLookAt(this.level, this.hitResult);
      this.profiler.push("gameMode");
      if (!this.pause && this.level != null) {
         this.gameMode.tick();
      }

      this.profiler.popPush("textures");
      boolean flag = this.level == null || this.level.tickRateManager().runsNormally();
      if (flag) {
         this.textureManager.tick();
      }

      if (this.screen == null && this.player != null) {
         if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen((Screen)null);
         } else if (this.player.isSleeping() && this.level != null) {
            this.setScreen(new InBedChatScreen());
         }
      } else {
         Screen $$5 = this.screen;
         if ($$5 instanceof InBedChatScreen) {
            InBedChatScreen inbedchatscreen = (InBedChatScreen)$$5;
            if (!this.player.isSleeping()) {
               inbedchatscreen.onPlayerWokeUp();
            }
         }
      }
      this.tickProvider = TickEvent.createNextProvider();

      for (IBaritone baritone : BaritoneAPI.getProvider().getAllBaritones()) {
         TickEvent.Type type = baritone.getPlayerContext().player() != null && baritone.getPlayerContext().world() != null
                 ? TickEvent.Type.IN
                 : TickEvent.Type.OUT;
         baritone.getGameEventHandler().onTick(this.tickProvider.apply(EventState.PRE, type));
      }
      if (this.screen != null) {
         this.missTime = 10000;
      }

      if (DebugSettings.global().executeInputsSynchronously.isEnabled()) {
         Queue<Runnable> inputEvents = ((IMouseKeyboard) this.mouseHandler).viaFabricPlus$getPendingScreenEvents();
         while (!inputEvents.isEmpty()) inputEvents.poll().run();

         inputEvents = ((IMouseKeyboard) this.keyboardHandler).viaFabricPlus$getPendingScreenEvents();
         while (!inputEvents.isEmpty()) inputEvents.poll().run();
      }

      if (this.screen != null) {
         Screen.wrapScreenError(() -> {
            this.screen.tick();
         }, "Ticking screen", this.screen.getClass().getCanonicalName());
      }

      if (!this.getDebugOverlay().showDebugScreen()) {
         this.gui.clearCache();
      }

      if (this.overlay == null && passEvents(this) == null) {
         this.profiler.popPush("Keybindings");
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            if (this.handleMissTime() > 0) {
               --this.missTime;
            }
         }
         this.handleKeybinds();
         if (this.handleMissTime() > 0) {
            --this.missTime;
         }
      }

      if (this.level != null) {
         this.profiler.popPush("gameRenderer");
         if (!this.pause) {
            this.gameRenderer.tick();
         }

         this.profiler.popPush("levelRenderer");
         if (!this.pause) {
            this.levelRenderer.tick();
         }

         this.profiler.popPush("level");
         if (!this.pause) {
            this.level.tickEntities();
            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(this.player);
            if (baritone != null) {
               // Intentionally call this after all entities have been updated. That way, any modification to rotations
               // can be recognized by other entity code. (Fireworks and Pigs, for example)
               baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
            }
         }
      } else if (this.gameRenderer.currentEffect() != null) {
         this.gameRenderer.shutdownEffect();
      }

      if (!this.pause) {
         this.musicManager.tick();
      }

      this.soundManager.tick(this.pause);
      if (this.level != null) {
         if (!this.pause) {
            if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
               Component component = Component.translatable("tutorial.socialInteractions.title");
               Component component1 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
               this.socialInteractionsToast = new TutorialToast(TutorialToast.Icons.SOCIAL_INTERACTIONS, component, component1, true);
               this.tutorial.addTimedToast(this.socialInteractionsToast, 160);
               this.options.joinedFirstServer = true;
               this.options.save();
            }

            this.tutorial.tick();

            try {
               this.level.tick(() -> true);
            } catch (Throwable throwable) {
               CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception in world tick");
               if (this.level == null) {
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Affected level");
                  crashreportcategory.setDetail("Problem", "Level is null!");
               } else {
                  this.level.fillReportDetails(crashreport);
               }

               throw new ReportedException(crashreport);
            }
         }

         this.profiler.popPush("animateTick");
         if (!this.pause && flag) {
            this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         this.profiler.popPush("particles");
         if (!this.pause && flag) {
            this.particleEngine.tick();
         }
      } else if (this.pendingConnection != null) {
         this.profiler.popPush("pendingConnection");
         this.pendingConnection.tick();
      }
      this.profiler.popPush("keyboard");
      this.keyboardHandler.tick();
      this.profiler.pop();

      for (IBaritone baritone : BaritoneAPI.getProvider().getAllBaritones()) {
         TickEvent.Type type = baritone.getPlayerContext().player() != null && baritone.getPlayerContext().world() != null
                 ? TickEvent.Type.IN
                 : TickEvent.Type.OUT;
         baritone.getGameEventHandler().onPostTick(this.tickProvider.apply(EventState.POST, type));
      }

       this.tickProvider = null;
   }

   private boolean isMultiplayerServer() {
      return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
   }

   private void handleKeybinds() {
      for(; this.options.keyTogglePerspective.consumeClick(); this.levelRenderer.needsUpdate()) {
         CameraType cameratype = this.options.getCameraType();
         this.options.setCameraType(this.options.getCameraType().cycle());
         if (cameratype.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
            this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
         }
      }

      while(this.options.keySmoothCamera.consumeClick()) {
         this.options.smoothCamera = !this.options.smoothCamera;
      }

      for(int i = 0; i < 9; ++i) {
         boolean flag = this.options.keySaveHotbarActivator.isDown();
         boolean flag1 = this.options.keyLoadHotbarActivator.isDown();
         if (this.options.keyHotbarSlots[i].consumeClick()) {
            if (this.player.isSpectator()) {
               this.gui.getSpectatorGui().onHotbarSelected(i);
            } else if (!this.player.isCreative() || this.screen != null || !flag1 && !flag) {
               this.player.getInventory().selected = i;
            } else {
               CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, i, flag1, flag);
            }
         }
      }

      while(this.options.keySocialInteractions.consumeClick()) {
         if (!this.isMultiplayerServer()) {
            this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            this.narrator.sayNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.tutorial.removeTimedToast(this.socialInteractionsToast);
               this.socialInteractionsToast = null;
            }

            this.setScreen(new SocialInteractionsScreen());
         }
      }

      while(this.options.keyInventory.consumeClick()) {
         if (this.gameMode.isServerControlledInventory()) {
            this.player.sendOpenInventory();
         } else {
            this.tutorial.onOpenInventory();
            if (DebugSettings.global().sendOpenInventoryPacket.isEnabled()) {
               final PacketWrapper clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, ProtocolTranslator.getPlayNetworkUserConnection());
               clientStatus.write(Type.VAR_INT, 2); // Open Inventory Achievement
               try {
                  clientStatus.scheduleSendToServer(Protocol1_12To1_11_1.class);
               } catch (Exception e) {
                  LOGGER.error("Failed to send via client status", e);
               }
            }
            this.setScreen(new InventoryScreen(this.player));
         }
      }

      while(this.options.keyAdvancements.consumeClick()) {
         this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
      }

      while(this.options.keySwapOffhand.consumeClick()) {
         if (!this.player.isSpectator()) {
            this.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
         }
      }

      while(this.options.keyDrop.consumeClick()) {
         if (!this.player.isSpectator() && this.player.drop(Screen.hasControlDown()) && ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15)) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      while(this.options.keyChat.consumeClick()) {
         this.openChatScreen("");
      }

      if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
         this.openChatScreen("/");
      }

      boolean flag2 = false;
      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isDown()) {
            this.gameMode.releaseUsingItem(this.player);
         }

         while(this.options.keyAttack.consumeClick()) {
         }

         while(this.options.keyUse.consumeClick()) {
         }

         while(this.options.keyPickItem.consumeClick()) {
         }
      } else {
         while(this.options.keyAttack.consumeClick()) {
            flag2 |= this.startAttack();
         }

         while(this.options.keyUse.consumeClick()) {
            this.startUseItem();
         }

         while(this.options.keyPickItem.consumeClick()) {
            this.pickBlock();
         }
      }

      if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
         this.startUseItem();
      }

      this.continueAttack(this.screen == null && !flag2 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
   }

   public ClientTelemetryManager getTelemetryManager() {
      return this.telemetryManager;
   }

   public double getGpuUtilization() {
      return this.gpuUtilization;
   }

   public ProfileKeyPairManager getProfileKeyPairManager() {
      return this.profileKeyPairManager;
   }

   public WorldOpenFlows createWorldOpenFlows() {
      return new WorldOpenFlows(this, this.levelSource);
   }

   public void doWorldLoad(LevelStorageSource.LevelStorageAccess pLevelStorage, PackRepository pPackRepository, WorldStem pWorldStem, boolean pNewWorld) {
      this.disconnect();
      this.progressListener.set((StoringChunkProgressListener)null);
      Instant instant = Instant.now();

      try {
         pLevelStorage.saveDataTag(pWorldStem.registries().compositeAccess(), pWorldStem.worldData());
         Services services = Services.create(this.authenticationService, this.gameDirectory);
         services.profileCache().setExecutor(this);
         SkullBlockEntity.setup(services, this);
         GameProfileCache.setUsesAuthentication(false);
         this.singleplayerServer = MinecraftServer.spin((p_231361_) -> {
            return new IntegratedServer(p_231361_, this, pLevelStorage, pPackRepository, pWorldStem, services, (p_231447_) -> {
               StoringChunkProgressListener storingchunkprogresslistener = new StoringChunkProgressListener(p_231447_ + 0);
               this.progressListener.set(storingchunkprogresslistener);
               return ProcessorChunkProgressListener.createStarted(storingchunkprogresslistener, this.progressTasks::add);
            });
         });
         this.isLocalServer = true;
         this.updateReportEnvironment(ReportEnvironment.local());
         this.quickPlayLog.setWorldData(QuickPlayLog.Type.SINGLEPLAYER, pLevelStorage.getLevelId(), pWorldStem.worldData().getLevelName());
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Starting integrated server");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Starting integrated server");
         crashreportcategory.setDetail("Level ID", pLevelStorage.getLevelId());
         crashreportcategory.setDetail("Level Name", () -> {
            return pWorldStem.worldData().getLevelName();
         });
         throw new ReportedException(crashreport);
      }

      while(this.progressListener.get() == null) {
         Thread.yield();
      }

      LevelLoadingScreen levelloadingscreen = new LevelLoadingScreen(this.progressListener.get());
      this.setScreen(levelloadingscreen);
      this.profiler.push("waitForServer");

      for(; !this.singleplayerServer.isReady() || this.overlay != null; this.handleDelayedCrash()) {
         levelloadingscreen.tick();
         this.runTick(false);

         try {
            Thread.sleep(16L);
         } catch (InterruptedException ignored) {
         }
      }

      this.profiler.pop();
      Duration duration = Duration.between(instant, Instant.now());
      SocketAddress socketaddress = this.singleplayerServer.getConnection().startMemoryChannel();
      Connection connection = Connection.connectToLocalServer(socketaddress);
      ProtocolTranslator.setTargetVersion(ProtocolTranslator.NATIVE_VERSION, true);
      ProtocolTranslator.injectPreviousVersionReset(connection.channel);
      connection.initiateServerboundPlayConnection(socketaddress.toString(), 0, new ClientHandshakePacketListenerImpl(connection, this, (ServerData)null, (Screen)null, pNewWorld, duration, (p_231442_) -> {
      }));
      connection.send(new ServerboundHelloPacket(this.getUser().getName(), this.getUser().getProfileId()));
      this.pendingConnection = connection;
   }

   public void setLevel(ClientLevel pLevelClient) {
      // If we're unloading the world but one doesn't exist, ignore it

       // mc.world changing is only the primary baritone

      BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
              new WorldEvent(
                      pLevelClient,
                      EventState.PRE
              )
      );
      ProgressScreen progressscreen = new ProgressScreen(true);
      progressscreen.progressStartNoAbort(Component.translatable("connect.joining"));
      this.updateScreenAndTick(progressscreen);
      this.level = pLevelClient;
      this.updateLevelInEngines(pLevelClient);
      if (!this.isLocalServer) {
         Services services = Services.create(this.authenticationService, this.gameDirectory);
         services.profileCache().setExecutor(this);
         SkullBlockEntity.setup(services, this);
         GameProfileCache.setUsesAuthentication(false);
      }
      // still fire event for both null, as that means we've just finished exiting a world

      // mc.world changing is only the primary baritone
      BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
              new WorldEvent(
                      pLevelClient,
                      EventState.POST
              )
      );
   }

   public void disconnect() {
      this.disconnect(new ProgressScreen(true));
   }

   public void disconnect(Screen pNextScreen) {
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null) {
         this.dropAllTasks();
         clientpacketlistener.close();
         this.clearDownloadedResourcePacks();
      }

      this.playerSocialManager.stopOnlineMode();
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      IntegratedServer integratedserver = this.singleplayerServer;
      this.singleplayerServer = null;
      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.clientLevelTeardownInProgress = true;

      try {
         this.updateScreenAndTick(pNextScreen);
         if (this.level != null) {
            if (integratedserver != null) {
               this.profiler.push("waitForServer");

               while(!integratedserver.isShutdown()) {
                  this.runTick(false);
               }

               this.profiler.pop();
            }

            this.gui.onDisconnected();
            this.isLocalServer = false;
         }

         this.level = null;
         this.updateLevelInEngines(null);
         this.player = null;
      } finally {
         this.clientLevelTeardownInProgress = false;
      }

      SkullBlockEntity.clear();
   }

   public void clearDownloadedResourcePacks() {
      this.downloadedPackSource.cleanupAfterDisconnect();
      this.runAllTasks();
   }

   public void clearClientLevel(Screen pNextScreen) {
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.clearLevel();
      }

      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.clientLevelTeardownInProgress = true;

      try {
         this.updateScreenAndTick(pNextScreen);
         this.gui.onDisconnected();
         this.level = null;
         this.updateLevelInEngines(null);
         this.player = null;
      } finally {
         this.clientLevelTeardownInProgress = false;
      }

      SkullBlockEntity.clear();
   }

   private void updateScreenAndTick(Screen pScreen) {
      this.profiler.push("forcedTick");
      this.soundManager.stop();
      this.cameraEntity = null;
      this.pendingConnection = null;
      this.setScreen(pScreen);
      this.runTick(false);
      this.profiler.pop();
   }

   public void forceSetScreen(Screen pScreen) {
      this.profiler.push("forcedTick");
      this.setScreen(pScreen);
      this.runTick(false);
      this.profiler.pop();
   }

   private void updateLevelInEngines(@Nullable ClientLevel pLevel) {
      this.levelRenderer.setLevel(pLevel);
      this.particleEngine.setLevel(pLevel);
      this.blockEntityRenderDispatcher.setLevel(pLevel);
      this.updateTitle();
   }

   private UserApiService.UserProperties userProperties() {
      return this.userPropertiesFuture.join();
   }

   public boolean telemetryOptInExtra() {
      return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
   }

   public boolean extraTelemetryAvailable() {
      return this.allowsTelemetry() && this.userProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
   }

   public boolean allowsTelemetry() {
      if (ModernUIClient.sRemoveTelemetrySession) {
         return false;
      }
      return SharedConstants.IS_RUNNING_IN_IDE ? false : this.userProperties().flag(UserFlag.TELEMETRY_ENABLED);
   }

   public boolean allowsMultiplayer() {
      return this.allowsMultiplayer && this.userProperties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null && !this.isNameBanned();
   }

   public boolean allowsRealms() {
      return this.userProperties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
   }

   @Nullable
   public BanDetails multiplayerBan() {
      return this.userProperties().bannedScopes().get("MULTIPLAYER");
   }

   public boolean isNameBanned() {
      ProfileResult profileresult = this.profileFuture.getNow((ProfileResult)null);
      return profileresult != null && profileresult.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
   }

   public boolean isBlocked(UUID pPlayerUUID) {
      if (this.getChatStatus().isChatAllowed(false)) {
         return this.playerSocialManager.shouldHideMessageFrom(pPlayerUUID);
      } else {
         return (this.player == null || !pPlayerUUID.equals(this.player.getUUID())) && !pPlayerUUID.equals(Util.NIL_UUID);
      }
   }

   public Minecraft.ChatStatus getChatStatus() {
      if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
         return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
      } else if (!this.allowsChat) {
         return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
      } else {
         return !this.userProperties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
      }
   }

   public final boolean isDemo() {
      return this.demo;
   }

   @Nullable
   public ClientPacketListener getConnection() {
      return this.player == null ? null : this.player.connection;
   }

   public static boolean renderNames() {
      return !instance.options.hideGui;
   }

   public static boolean useFancyGraphics() {
      return instance.options.graphicsMode().get().getId() >= GraphicsStatus.FANCY.getId();
   }

   public static boolean useShaderTransparency() {
      return !instance.gameRenderer.isPanoramicMode() && instance.options.graphicsMode().get().getId() >= GraphicsStatus.FABULOUS.getId();
   }

   public static boolean useAmbientOcclusion() {
      return instance.options.ambientOcclusion().get();
   }

   private void pickBlock() {
      if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
         boolean flag = this.player.getAbilities().instabuild;
         BlockEntity blockentity = null;
         HitResult.Type hitresult$type = this.hitResult.getType();
         ItemStack itemstack;
         if (hitresult$type == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)this.hitResult).getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.isAir()) {
               return;
            }

            Block block = blockstate.getBlock();
            itemstack = block.getCloneItemStack(this.level, blockpos, blockstate);
            if (itemstack.isEmpty()) {
               return;
            }

            if (flag && Screen.hasControlDown() && blockstate.hasBlockEntity()) {
               blockentity = this.level.getBlockEntity(blockpos);
            }
         } else {
            if (hitresult$type != HitResult.Type.ENTITY || !flag) {
               return;
            }

            Entity entity = ((EntityHitResult)this.hitResult).getEntity();
            itemstack = entity.getPickResult();
            if (itemstack == null) {
               return;
            }
         }

         if (itemstack.isEmpty()) {
            String s = "";
            if (hitresult$type == HitResult.Type.BLOCK) {
               s = BuiltInRegistries.BLOCK.getKey(this.level.getBlockState(((BlockHitResult)this.hitResult).getBlockPos()).getBlock()).toString();
            } else if (hitresult$type == HitResult.Type.ENTITY) {
               s = BuiltInRegistries.ENTITY_TYPE.getKey(((EntityHitResult)this.hitResult).getEntity().getType()).toString();
            }

            LOGGER.warn("Picking on: [{}] {} gave null item", hitresult$type, s);
         } else {
            Inventory inventory = this.player.getInventory();
            if (blockentity != null) {
               this.addCustomNbtData(itemstack, blockentity);
            }

            int i = inventory.findSlotMatchingItem(itemstack);
            if (flag) {
               if (ItemRegistryDiff.keepItem(itemstack.getItem())) {
                  inventory.setPickedItem(itemstack);
               }
               //inventory.setPickedItem(itemstack);
               if (!itemstack.isEmpty()) {
                  gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
               }
               //this.gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
            } else if (i != -1) {
               if (Inventory.isHotbarSlot(i)) {
                  inventory.selected = i;
               } else {
                  this.gameMode.handlePickItem(i);
               }
            }

         }
      }
   }

   private void addCustomNbtData(ItemStack pStack, BlockEntity pBe) {
      CompoundTag compoundtag = pBe.saveWithFullMetadata();
      BlockItem.setBlockEntityData(pStack, pBe.getType(), compoundtag);
      if (pStack.getItem() instanceof PlayerHeadItem && compoundtag.contains("SkullOwner")) {
         CompoundTag compoundtag3 = compoundtag.getCompound("SkullOwner");
         CompoundTag compoundtag4 = pStack.getOrCreateTag();
         compoundtag4.put("SkullOwner", compoundtag3);
         CompoundTag compoundtag2 = compoundtag4.getCompound("BlockEntityTag");
         compoundtag2.remove("SkullOwner");
         compoundtag2.remove("x");
         compoundtag2.remove("y");
         compoundtag2.remove("z");
      } else {
         CompoundTag compoundtag1 = new CompoundTag();
         ListTag listtag = new ListTag();
         listtag.add(StringTag.valueOf("\"(+NBT)\""));
         compoundtag1.put("Lore", listtag);
         pStack.addTagElement("display", compoundtag1);
      }
   }

   public CrashReport fillReport(CrashReport pTheCrash) {
      SystemReport systemreport = pTheCrash.getSystemReport();
      fillSystemReport(systemreport, this, this.languageManager, this.launchedVersion, this.options);
      this.fillUptime(pTheCrash.addCategory("Uptime"));
      if (this.level != null) {
         this.level.fillReportDetails(pTheCrash);
      }

      if (this.singleplayerServer != null) {
         this.singleplayerServer.fillSystemReport(systemreport);
      }

      this.reloadStateTracker.fillCrashReport(pTheCrash);
      return pTheCrash;
   }

   public static void fillReport(@Nullable Minecraft pMinecraft, @Nullable LanguageManager pLanguageManager, String pLaunchVersion, @Nullable Options pOptions, CrashReport pReport) {
      SystemReport systemreport = pReport.getSystemReport();
      fillSystemReport(systemreport, pMinecraft, pLanguageManager, pLaunchVersion, pOptions);
   }

   private static String formatSeconds(double pSeconds) {
      return String.format(Locale.ROOT, "%.3fs", pSeconds);
   }

   private void fillUptime(CrashReportCategory pCategory) {
      pCategory.setDetail("JVM uptime", () -> formatSeconds((double)ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0D));
      pCategory.setDetail("Wall uptime", () -> formatSeconds((double)(System.currentTimeMillis() - this.clientStartTimeMs) / 1000.0D));
      pCategory.setDetail("High-res time", () -> formatSeconds((double)Util.getMillis() / 1000.0D));
      pCategory.setDetail("Client ticks", () -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.clientTickCount, (double)this.clientTickCount / 20.0D));
   }

   private static SystemReport fillSystemReport(SystemReport pReport, @Nullable Minecraft pMinecraft, @Nullable LanguageManager pLanguageManager, String pLaunchVersion, @Nullable Options pOptions) {
      pReport.setDetail("Launched Version", () -> pLaunchVersion);
      String s = getLauncherBrand();
      if (s != null) {
         pReport.setDetail("Launcher name", s);
      }

      pReport.setDetail("Backend library", RenderSystem::getBackendDescription);
      pReport.setDetail("Backend API", RenderSystem::getApiDescription);
      pReport.setDetail("Window size", () -> pMinecraft != null ? pMinecraft.window.getWidth() + "x" + pMinecraft.window.getHeight() : "<not initialized>");
      pReport.setDetail("GL Caps", RenderSystem::getCapsString);
      pReport.setDetail("GL debug messages", () -> GlDebug.isDebugEnabled() ? String.join("\n", GlDebug.getLastOpenGlDebugMessages()) : "<disabled>");
      pReport.setDetail("Using VBOs", () -> "Yes");
      pReport.setDetail("Is Modded", () -> checkModStatus().fullDescription());
      pReport.setDetail("Universe", () -> pMinecraft != null ? Long.toHexString(pMinecraft.canary) : "404");
      pReport.setDetail("Type", "Client (map_client.txt)");
      if (pOptions != null) {
         if (pMinecraft != null) {
            String s1 = pMinecraft.getGpuWarnlistManager().getAllWarnings();
            if (s1 != null) {
               pReport.setDetail("GPU Warnings", s1);
            }
         }

         pReport.setDetail("Graphics mode", pOptions.graphicsMode().get().toString());
         pReport.setDetail("Render Distance", pOptions.getEffectiveRenderDistance() + "/" + pOptions.renderDistance().get() + " chunks");
         pReport.setDetail("Resource Packs", () -> {
            StringBuilder stringbuilder = new StringBuilder();

            for(String s2 : pOptions.resourcePacks) {
               if (stringbuilder.length() > 0) {
                  stringbuilder.append(", ");
               }

               stringbuilder.append(s2);
               if (pOptions.incompatibleResourcePacks.contains(s2)) {
                  stringbuilder.append(" (incompatible)");
               }
            }

            return stringbuilder.toString();
         });
      }

      if (pLanguageManager != null) {
         pReport.setDetail("Current Language", pLanguageManager::getSelected);
      }

      pReport.setDetail("Locale", String.valueOf(Locale.getDefault()));
      pReport.setDetail("CPU", GlUtil::getCpuInfo);
      return pReport;
   }

   public static Minecraft getInstance() {
      return instance;
   }

   public CompletableFuture<Void> delayTextureReload() {
      return this.submit(() -> this.reloadResourcePacks()).thenCompose((p_231391_) -> p_231391_);
   }

   public void updateReportEnvironment(ReportEnvironment pReportEnvironment) {
      if (!this.reportingContext.matches(pReportEnvironment)) {
         this.reportingContext = ReportingContext.create(pReportEnvironment, this.userApiService);
      }

   }

   @Nullable
   public ServerData getCurrentServer() {
      return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
   }

   public boolean isLocalServer() {
      return this.isLocalServer;
   }

   public boolean hasSingleplayerServer() {
      return this.isLocalServer && this.singleplayerServer != null;
   }

   @Nullable
   public IntegratedServer getSingleplayerServer() {
      return this.singleplayerServer;
   }

   public boolean isSingleplayer() {
      IntegratedServer integratedserver = this.getSingleplayerServer();
      return integratedserver != null && !integratedserver.isPublished();
   }

   public boolean isLocalPlayer(UUID pUuid) {
      return pUuid.equals(this.getUser().getProfileId());
   }

   public User getUser() {
      return this.user;
   }

   public GameProfile getGameProfile() {
      ProfileResult profileresult = this.profileFuture.join();
      return profileresult != null ? profileresult.profile() : new GameProfile(this.user.getProfileId(), this.user.getName());
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public PackRepository getResourcePackRepository() {
      return this.resourcePackRepository;
   }

   public VanillaPackResources getVanillaPackResources() {
      return this.vanillaPackResources;
   }

   public DownloadedPackSource getDownloadedPackSource() {
      return this.downloadedPackSource;
   }

   public Path getResourcePackDirectory() {
      return this.resourcePackDirectory;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation pLocation) {
      return this.modelManager.getAtlas(pLocation)::getSprite;
   }

   public boolean is64Bit() {
      return this.is64bit;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public GpuWarnlistManager getGpuWarnlistManager() {
      return this.gpuWarnlistManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public Music getSituationalMusic() {
      Music music = Optionull.map(this.screen, Screen::getBackgroundMusic);
      if (music != null) {
         return music;
      } else if (this.player != null) {
         if (this.player.level().dimension() == Level.END) {
            return this.gui.getBossOverlay().shouldPlayMusic() ? Musics.END_BOSS : Musics.END;
         } else {
            Holder<Biome> holder = this.player.level().getBiome(this.player.blockPosition());
            if (!this.musicManager.isPlayingMusic(Musics.UNDER_WATER) && (!this.player.isUnderWater() || !holder.is(BiomeTags.PLAYS_UNDERWATER_MUSIC))) {
               return this.player.level().dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly ? Musics.CREATIVE : holder.value().getBackgroundMusic().orElse(Musics.GAME);
            } else {
               return Musics.UNDER_WATER;
            }
         }
      } else {
         return Musics.MENU;
      }
   }

   public MinecraftSessionService getMinecraftSessionService() {
      return this.minecraftSessionService;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   @Nullable
   public Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(Entity pViewingEntity) {
      this.cameraEntity = pViewingEntity;
      this.gameRenderer.checkEntityPostEffect(pViewingEntity);
   }

   public boolean shouldEntityAppearGlowing(Entity pEntity) {
      return pEntity.isCurrentlyGlowing() || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && pEntity.getType() == EntityType.PLAYER;
   }

   protected Thread getRunningThread() {
      return this.gameThread;
   }

   protected Runnable wrapRunnable(Runnable pRunnable) {
      return pRunnable;
   }

   protected boolean shouldRun(Runnable pRunnable) {
      return true;
   }

   public BlockRenderDispatcher getBlockRenderer() {
      return this.blockRenderer;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public <T> SearchTree<T> getSearchTree(SearchRegistry.Key<T> pKey) {
      return this.searchRegistry.getTree(pKey);
   }

   public <T> void populateSearchTree(SearchRegistry.Key<T> pKey, List<T> pValues) {
      this.searchRegistry.populate(pKey, pValues);
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public float getFrameTime() {
      return this.timer.partialTick;
   }

   public float getDeltaFrameTime() {
      return this.timer.tickDelta;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean showOnlyReducedInfo() {
      return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
   }

   public ToastComponent getToasts() {
      return this.toast;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public boolean isWindowActive() {
      return this.windowActive;
   }

   public HotbarManager getHotbarManager() {
      return this.hotbarManager;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public PaintingTextureManager getPaintingTextures() {
      return this.paintingTextures;
   }

   public MobEffectTextureManager getMobEffectTextures() {
      return this.mobEffectTextures;
   }

   public GuiSpriteManager getGuiSprites() {
      return this.guiSprites;
   }

   public void setWindowActive(boolean pFocused) {
      this.windowActive = pFocused;
   }

   public Component grabPanoramixScreenshot(File pGameDirectory, int pWidth, int pHeight) {
      int i = this.window.getWidth();
      int j = this.window.getHeight();
      RenderTarget rendertarget = new TextureTarget(pWidth, pHeight, true, ON_OSX);
      float f = this.player.getXRot();
      float f1 = this.player.getYRot();
      float f2 = this.player.xRotO;
      float f3 = this.player.yRotO;
      this.gameRenderer.setRenderBlockOutline(false);

      MutableComponent mutablecomponent;
      try {
         this.gameRenderer.setPanoramicMode(true);
         this.levelRenderer.graphicsChanged();
         this.window.setWidth(pWidth);
         this.window.setHeight(pHeight);

         for(int k = 0; k < 6; ++k) {
            switch (k) {
               case 0:
                  this.player.setYRot(f1);
                  this.player.setXRot(0.0F);
                  break;
               case 1:
                  this.player.setYRot((f1 + 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 2:
                  this.player.setYRot((f1 + 180.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 3:
                  this.player.setYRot((f1 - 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 4:
                  this.player.setYRot(f1);
                  this.player.setXRot(-90.0F);
                  break;
               case 5:
               default:
                  this.player.setYRot(f1);
                  this.player.setXRot(90.0F);
            }

            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
            rendertarget.bindWrite(true);
            this.gameRenderer.renderLevel(1.0F, 0L, new PoseStack());

            try {
               Thread.sleep(10L);
            } catch (InterruptedException interruptedexception) {
            }

            Screenshot.grab(pGameDirectory, "panorama_" + k + ".png", rendertarget, (p_231415_) -> {
            });
         }

         Component component = Component.literal(pGameDirectory.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((p_231426_) -> p_231426_.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, pGameDirectory.getAbsolutePath())));
         return Component.translatable("screenshot.success", component);
      } catch (Exception exception) {
         LOGGER.error("Couldn't save image", (Throwable)exception);
         mutablecomponent = Component.translatable("screenshot.failure", exception.getMessage());
      } finally {
         this.player.setXRot(f);
         this.player.setYRot(f1);
         this.player.xRotO = f2;
         this.player.yRotO = f3;
         this.gameRenderer.setRenderBlockOutline(true);
         this.window.setWidth(i);
         this.window.setHeight(j);
         rendertarget.destroyBuffers();
         this.gameRenderer.setPanoramicMode(false);
         this.levelRenderer.graphicsChanged();
         this.getMainRenderTarget().bindWrite(true);
      }

      return mutablecomponent;
   }

   private Component grabHugeScreenshot(File pGameDirectory, int pColumnWidth, int pRowHeight, int pWidth, int pHeight) {
      try {
         ByteBuffer bytebuffer = GlUtil.allocateMemory(pColumnWidth * pRowHeight * 3);
         Screenshot screenshot = new Screenshot(pGameDirectory, pWidth, pHeight, pRowHeight);
         float f = (float)pWidth / (float)pColumnWidth;
         float f1 = (float)pHeight / (float)pRowHeight;
         float f2 = f > f1 ? f : f1;

         for(int i = (pHeight - 1) / pRowHeight * pRowHeight; i >= 0; i -= pRowHeight) {
            for(int j = 0; j < pWidth; j += pColumnWidth) {
               RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
               float f3 = (float)(pWidth - pColumnWidth) / 2.0F * 2.0F - (float)(j * 2);
               float f4 = (float)(pHeight - pRowHeight) / 2.0F * 2.0F - (float)(i * 2);
               f3 /= (float)pColumnWidth;
               f4 /= (float)pRowHeight;
               this.gameRenderer.renderZoomed(f2, f3, f4);
               bytebuffer.clear();
               RenderSystem.pixelStore(3333, 1);
               RenderSystem.pixelStore(3317, 1);
               RenderSystem.readPixels(0, 0, pColumnWidth, pRowHeight, 32992, 5121, bytebuffer);
               screenshot.addRegion(bytebuffer, j, i, pColumnWidth, pRowHeight);
            }

            screenshot.saveRow();
         }

         File file1 = screenshot.close();
         GlUtil.freeMemory(bytebuffer);
         Component component = Component.literal(file1.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((p_231379_) -> p_231379_.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file1.getAbsolutePath())));
         return Component.translatable("screenshot.success", component);
      } catch (Exception exception) {
         LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
         return Component.translatable("screenshot.failure", exception.getMessage());
      }
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   @Nullable
   public StoringChunkProgressListener getProgressListener() {
      return this.progressListener.get();
   }

   public SplashManager getSplashManager() {
      return this.splashManager;
   }

   @Nullable
   public Overlay getOverlay() {
      return this.overlay;
   }

   public PlayerSocialManager getPlayerSocialManager() {
      return this.playerSocialManager;
   }

   public boolean renderOnThread() {
      return false;
   }

   public Window getWindow() {
      return this.window;
   }

   public DebugScreenOverlay getDebugOverlay() {
      return this.gui.getDebugOverlay();
   }

   public RenderBuffers renderBuffers() {
      return this.renderBuffers;
   }

   public void updateMaxMipLevel(int pMipMapLevel) {
      this.modelManager.updateMaxMipLevel(pMipMapLevel);
   }

   public EntityModelSet getEntityModels() {
      return this.entityModels;
   }

   public boolean isTextFilteringEnabled() {
      return this.userProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
   }

   public void prepareForMultiplayer() {
      this.playerSocialManager.startOnlineMode();
      this.getProfileKeyPairManager().prepareKeyPair();
   }

   public Realms32BitWarningStatus getRealms32BitWarningStatus() {
      return this.realms32BitWarningStatus;
   }

   @Nullable
   public SignatureValidator getProfileKeySignatureValidator() {
      return SignatureValidator.from(this.authenticationService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);
   }

   public boolean canValidateProfileKeys() {
      return !this.authenticationService.getServicesKeySet().keys(ServicesKeyType.PROFILE_KEY).isEmpty();
   }

   public InputType getLastInputType() {
      return this.lastInputType;
   }

   public void setLastInputType(InputType pLastInputType) {
      this.lastInputType = pLastInputType;
   }

   public GameNarrator getNarrator() {
      return this.narrator;
   }

   public ChatListener getChatListener() {
      return this.chatListener;
   }

   public ReportingContext getReportingContext() {
      return this.reportingContext;
   }

   public RealmsDataFetcher realmsDataFetcher() {
      return this.realmsDataFetcher;
   }

   public QuickPlayLog quickPlayLog() {
      return this.quickPlayLog;
   }

   public CommandHistory commandHistory() {
      return this.commandHistory;
   }

   public DirectoryValidator directoryValidator() {
      return this.directoryValidator;
   }

   private float getTickTargetMillis(float p_311597_) {
      if (this.level != null) {
         TickRateManager tickratemanager = this.level.tickRateManager();
         if (tickratemanager.runsNormally()) {
            return Math.max(p_311597_, tickratemanager.millisecondsPerTick());
         }
      }

      return p_311597_;
   }
   public void setUser(User user) {
      this.user = user;
   }
   public void setSessionService(MinecraftSessionService service) {
      this.minecraftSessionService = service;
   }
   public void setUserApiService(UserApiService service) {
      this.userApiService = service;
   }
   public void setPlayerSocialManager(PlayerSocialManager manager) {
      this.playerSocialManager = manager;
   }
   public void setSkinManager(SkinManager manager) {
      this.skinManager = manager;
   }

   @Nullable
   public static String getLauncherBrand() {
      return System.getProperty("minecraft.launcher.brand");
   }

   @OnlyIn(Dist.CLIENT)
   public enum ChatStatus {
      ENABLED(CommonComponents.EMPTY) {
         public boolean isChatAllowed(boolean p_168045_) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean p_168051_) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean p_168057_) {
            return p_168057_;
         }
      },
      DISABLED_BY_PROFILE(Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean p_168063_) {
            return p_168063_;
         }
      };

      static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
      private final Component message;

      ChatStatus(Component pMessage) {
         this.message = pMessage;
      }

      public Component getMessage() {
         return this.message;
      }

      public abstract boolean isChatAllowed(boolean pIsLocalServer);
   }

   @OnlyIn(Dist.CLIENT)
   private static record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
   }
}