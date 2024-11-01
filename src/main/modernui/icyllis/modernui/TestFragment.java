/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui;

import icyllis.modernui.animation.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.audio.*;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.text.FontFamily;
import icyllis.modernui.graphics.text.LineBreakConfig;
import icyllis.modernui.material.MaterialCheckBox;
import icyllis.modernui.material.MaterialRadioButton;
import icyllis.modernui.resources.SystemTheme;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.FloatProperty;
import icyllis.modernui.view.*;
import icyllis.modernui.view.ViewGroup.LayoutParams;
import icyllis.modernui.widget.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/**
 * Usability test, don't use.
 */
public class TestFragment extends Fragment {

    //public static SpectrumGraph sSpectrumGraph;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.DEBUG);

        try (ModernUI app = new ModernUI()) {
            app.run(new TestFragment());
        }
        AudioManager.getInstance().close();
        System.gc();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getParentFragmentManager().beginTransaction()
                .setPrimaryNavigationFragment(this)
                .commit();
    }

    @Override
    public void onCreate(@Nullable DataSet savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager().beginTransaction()
                .replace(660, new FragmentA(), null)
                .commit();

        /*CompletableFuture.runAsync(() -> {
            String text = "My name is van";
            var tp = new TextPaint();
            tp.setTextStyle(TextPaint.BOLD);
            var shapedText = TextShaper.shapeText(text, 1, text.length() - 2, 0, text.length(),
                    TextDirectionHeuristics.FIRSTSTRONG_LTR, tp);
            LOGGER.info("Shape \"{}\"\n{}\nMemory Usage: {} bytes", text, shapedText, shapedText.getMemoryUsage());
            text = "y";
            var adv = tp.getTypeface().getFamilies().get(0).getClosestMatch(FontPaint.BOLD)
                    .doSimpleLayout(text.toCharArray(), 0, 1, tp.getInternalPaint(), null, null, 0, 0);
            LOGGER.info("y: adv {}", adv);
        }).exceptionally(e -> {
            LOGGER.info("Shape", e);
            return null;
        });*/

        AudioManager.getInstance().initialize();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        var base = new ScrollView(getContext());
        base.setId(660);

        base.setBackground(new Drawable() {
            long lastTime = AnimationUtils.currentAnimationTimeMillis();

            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint paint = Paint.obtain();
                Rect b = getBounds();
                paint.setRGBA(8, 8, 8, 80);
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8, paint);
                paint.recycle();

                /*SpectrumGraph graph = sSpectrumGraph;
                long time = AnimationUtils.currentAnimationTimeMillis();
                long delta = time - lastTime;
                lastTime = time;
                if (graph != null) {
                    if (graph.update(delta)) {
                        float cx = getBounds().centerX(), cy = getBounds().centerY();
                        graph.draw(canvas, cx, cy);
                        invalidateSelf();
                    }
                }*/
            }
        });
        {
            var params = new FrameLayout.LayoutParams(base.dp(960), base.dp(540));
            params.gravity = Gravity.CENTER;
            base.setLayoutParams(params);
        }
        //base.setRotation(30);
        container.setClipChildren(true);
        return base;
    }

    public static class FragmentA extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable DataSet savedInstanceState) {
            LinearLayout content = new TestLinearLayout(getContext());
            //content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            content.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            content.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEY_E && event.getAction() == KeyEvent.ACTION_UP) {
                    /*getParentFragmentManager().beginTransaction()
                            .replace(getId(), new FragmentB())
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .commit();*/
                    return true;
                }
                return false;
            });

            LOGGER.info("{} onCreateView(), id={}", getClass().getSimpleName(), getId());

            return content;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LOGGER.info("{} onDestroy()", getClass().getSimpleName());
        }
    }

    public static class FragmentB extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable DataSet savedInstanceState) {
            var tv = new TextView(getContext());
            tv.setText("My name is Van, I'm an arist, a performance artist.");
            return tv;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LOGGER.info("FragmentB onDestroy()");
        }
    }

    private static class TestView extends View {

        public TestView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            //canvas.drawRing(100, 20, 5, 8);
            // 3


            /*//RenderHelper.setupGuiFlatDiffuseLighting();
            //GL11.glColor4d(1, 1, 1, 1);
            //GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            RenderSystem.disableDepthTest();

            MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
            RenderSystem.multMatrix(Matrix4f.perspective(90.0D,
                    (float) mainWindow.getFramebufferWidth() / mainWindow.getFramebufferHeight(),
                    1.0F, 100.0F));
            //RenderSystem.viewport(0, 0, mainWindow.getFramebufferWidth(), mainWindow.getFramebufferHeight());
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(-2.8f, -1.0f, -1.8f);
            GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
            //GL11.glTranslatef(0, 3, 1984);
            //GL11.glRotatef((canvas.getDrawingTime() / 10f) % 360 - 180, 0, 1, 0);
            GL11.glRotatef(12, 0, 1, 0);
            *//*if ((canvas.getDrawingTime() ^ 127) % 40 == 0) {
             *//**//*float[] pj = new float[16];
                GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, pj);
                ModernUI.LOGGER.info(Arrays.toString(pj));
                GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, pj);
                ModernUI.LOGGER.info(Arrays.toString(pj));*//**//*
                ModernUI.LOGGER.info(GL11.glGetBoolean(GL30.GL_RESCALE_NORMAL));
            }*//*
            ClientPlayerEntity player = Minecraft.getInstance().player;
            canvas.setColor(170, 220, 240, 128);
            if (player != null) {
                canvas.drawRoundedRect(0, 25, player.getHealth() * 140 / player.getMaxHealth(), 39, 4);
            }
            *//*canvas.setAlpha(255);
            canvas.drawRoundedFrame(1, 26, 141, 40, 4);*//*
             *//*canvas.setColor(53, 159, 210, 192);
            canvas.drawRoundedFrame(0, 25, 140, 39, 4);*//*
            if (player != null) {
                canvas.resetColor();
                canvas.setTextAlign(TextAlign.RIGHT);
                canvas.drawText(decimalFormat.format(player.getHealth()) + " / " + decimalFormat.format(player
                .getMaxHealth()), 137, 28);
            }
            RenderSystem.enableDepthTest();
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            //GL11.glEnable(GL11.GL_CULL_FACE);
            //RenderHelper.setupGui3DDiffuseLighting();

            //canvas.drawRoundedRect(0, 25, 48, 45, 6);*/
        }
    }

    public static class TestLinearLayout extends LinearLayout {

        private float c = 10;
        private float f = 0;

        /*private final Animation cAnim;

        private final Animation circleAnimation1;
        private final Animation circleAnimation2;
        private final Animation circleAnimation3;
        private final Animation circleAnimation4;
        private final Animation iconRadiusAni;
        private final Animation arcStartAni;
        private final Animation arcEndAni;*/

        private static final FloatBuffer sLinePoints = FloatBuffer.allocate(16);
        private static final IntBuffer sLineColors = IntBuffer.allocate(sLinePoints.capacity() / 2);

        private static final FloatBuffer sTrianglePoints = FloatBuffer.allocate(12);
        private static final IntBuffer sTriangleColors = IntBuffer.allocate(sTrianglePoints.capacity() / 2);

        static {
            sLinePoints
                    .put(100).put(100)
                    .put(110).put(200)
                    .put(120).put(100)
                    .put(130).put(300)
                    .put(140).put(100)
                    .put(150).put(400)
                    .put(160).put(100)
                    .put(170).put(500)
                    .flip();
            sLineColors
                    .put(0xAAFF0000)
                    .put(0xFFFF00FF)
                    .put(0xAA0000FF)
                    .put(0xFF00FF00)
                    .put(0xAA00FFFF)
                    .put(0xFF00FF00)
                    .put(0xAAFFFF00)
                    .put(0xFFFFFFFF)
                    .flip();
            sTrianglePoints
                    .put(420).put(20)
                    .put(420).put(100)
                    .put(490).put(60)
                    .put(300).put(130)
                    .put(250).put(180)
                    .put(350).put(180)
                    .flip();
            sTriangleColors
                    .put(0xAAFF0000)
                    .put(0xFFFF00FF)
                    .put(0xAA0000FF)
                    .put(0xAA00FFFF)
                    .put(0xFF00FF00)
                    .put(0xAAFFFF00)
                    .flip();
        }

        private final Animator mRoundRectLenAnim;
        //private final Animation roundRectAlphaAni;

        private float circleAcc1;
        private float circleAcc2;
        private float circleAcc3;
        private float circleAcc4;
        private float iconRadius = 40;
        private float arcStart = 0;
        private float arcEnd = 0;

        private float mRoundRectLen = 0;
        private float roundRectAlpha = 0;
        private float mSmoothRadius = 0;

        private boolean b;

        private int ticks;

        private final TextView mTextView;

        PopupWindow mPopupWindow = new PopupWindow();

        ObjectAnimator mGoodAnim;

        ImageShader mTestImageShader;
        LinearGradient mTestLinearGrad;
        AngularGradient mTestAngularGrad;

        public TestLinearLayout(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setDividerDrawable(new Drawable() {
                @Override
                public void draw(@Nonnull Canvas canvas) {
                    Paint paint = Paint.obtain();
                    paint.setRGBA(192, 192, 192, 128);
                    canvas.drawRect(getBounds(), paint);
                    paint.recycle();
                }

                @Override
                public int getIntrinsicHeight() {
                    return 2;
                }
            });
            setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);

            setPadding(dp(12), dp(12), dp(12), dp(12));

            setDividerPadding(dp(8));

            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);

            String text;
            text = "\t\t\u0639\u0646\u062f\u0645\u0627\u0020\u064a\u0631\u064a\u062f\u0020\u0627\u0644\u0639\u0627" +
                    "\u0644" +
                    "\u0645\u0020\u0623\u0646\u0020\u202a\u064a\u062a\u0643\u0644\u0651\u0645\u0020\u202c\u0020\u060c" +
                    "\u0020\u0641\u0647\u0648\u0020\u064a\u062a\u062d\u062f\u0651\u062b\u0020\u0628\u0644\u063a\u0629" +
                    "\u0020\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u002e\u0020\u062a\u0633\u062c\u0651\u0644\u0020" +
                    "\u0627\u0644\u0622\u0646\u0020\u0644\u062d\u0636\u0648\u0631\u0020\u0627\u0644\u0645\u0624\u062a" +
                    "\u0645\u0631\u0020\u0627\u0644\u062f\u0648\u0644\u064a\u0020\u0627\u0644\u0639\u0627\u0634\u0631" +
                    "\u0020\u0644\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u0020\u0028\u0055\u006e\u0069\u0063\u006f" +
                    "\u0064\u0065\u0020\u0043\u006f\u006e\u0066\u0065\u0072\u0065\u006e\u0063\u0065\u0029\n";
            int firstPara = text.length();
            text += "\t\t红 日（迫真）\n";
            int secondsPara = text.length();
            text += "\t\tMy name is Van, I'm 30 years old, and I'm from Japan. I'm an artist, I'm a performance " +
                    "artist. " +
                    "I'm hired for people to fulfill their fantasies, their deep dark fantasies.\n" +
                    "\t\t\u4f60\u770b\u8fd9\u4e2a\u5f6c\u5f6c\u0020\u624d\u559d\u51e0\u7f50\u0020\u5c31\u9189" +
                    "\u4e86\u002e\u002e\u002e\u771f\u7684\u592a\u900a\u529b" +
                    "\uff1b\u54e6\uff0c\u542c\u4f60" +
                    "\u90a3\u4e48\u8bf4\u0020\u4f60\u5f88\u52c7" +
                    "\u54e6\uff1b\u5f00\u73a9\u7b11\uff0c\u6211" +
                    "\u8d85\u52c7\u7684\u597d\u4e0d\u597d\u0020\u6211\u8d85\u4f1a\u559d\u7684\u5566\n";
            text += "Oops, your ";
            int emojiSt = text.length();
            text += "\uD83D\uDC34 died\n";
            text += "\t\t\u09b9\u09cd\u09af\u09be\u09b2\u09cb\u0020\u0645\u0631\u062d\u0628\u0627\u0020\ud808\udd99" +
                    "\ud808" +
                    "\udd99";

            TextView tv = new TextView(getContext());
            tv.setLayoutParams(new LayoutParams(tv.dp(640), WRAP_CONTENT));
            tv.setLineBreakWordStyle(LineBreakConfig.LINE_BREAK_WORD_STYLE_BREAK_ALL);

            Spannable spannable = new SpannableString(text);
            spannable.setSpan(new ForegroundColorSpan(0xfff699b4), text.length() - 54, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(1.15f), text.length() - 99, text.length() - 30,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), text.length() - 50, text.length() - 40,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new URLSpan("https://www.bilibili.com/video/BV1qm411Q7LX"), firstPara, secondsPara - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(0xff4f81bd), firstPara, secondsPara - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new SuperscriptSpan(), firstPara + 4, firstPara + 5,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new UnderlineSpan(), text.length() / 2, text.length() / 4 * 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StrikethroughSpan(), text.length() / 4 * 3, text.length(),
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            CompletableFuture.runAsync(() -> {
                long startNanos = System.nanoTime();
                var precomputed = PrecomputedText.create(spannable, tv.getTextMetricsParams());
                long usedNanos = System.nanoTime() - startNanos;
                LOGGER.info("Precomputed text in {} microseconds", usedNanos / 1000);
                tv.post(() -> tv.setText(precomputed, TextView.BufferType.SPANNABLE));
            });
            /*try {
                Image image = ImageStore.getInstance().create(
                        FileChannel.open(Path.of("F:/Photoshop/AppleEmoji/horse-face_1f434.png"),
                                StandardOpenOption.READ));
                if (image != null) {
                    ImageSpan span = new ImageSpan(image);
                    span.getDrawable().setBounds(0, 0, sp(24), sp(24));
                    spannable.setSpan(span, emojiSt, emojiSt + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (IOException ignored) {
            }*/
            tv.setLinksClickable(true);
            tv.setTextIsSelectable(true);
            tv.setTextAlignment(TEXT_ALIGNMENT_GRAVITY);

            mTextView = tv;
            //setRotationY(-30);
            //setRotationX(30);

            ObjectAnimator anim;
            {
                var pvh1 = PropertyValuesHolder.ofFloat(ROTATION, 0, 2880);
                var pvh2 = PropertyValuesHolder.ofFloat(ROTATION_Y, 0, 720);
                var pvh3 = PropertyValuesHolder.ofFloat(ROTATION_X, 0, 1440);
                anim = ObjectAnimator.ofPropertyValuesHolder(this, pvh1, pvh2, pvh3);
                anim.setDuration(12000);
                anim.setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE);
                //anim.setRepeatCount(ValueAnimator.INFINITE);
                //anim.start();
                mGoodAnim = anim;
            }

            try (Bitmap bitmap = BitmapFactory.decodePath(Path.of("E:/flux_core.png"))) {
                Image image = Image.createTextureFromBitmap(bitmap);
                if (image != null) {
                    Matrix scalingMatrix = new Matrix();
                    scalingMatrix.setScale(3, 3);
                    mTestImageShader = new ImageShader(image, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT,
                            ImageShader.FILTER_POINT, scalingMatrix);
                } else {
                    LOGGER.warn("Failed to create image");
                }
            } catch (IOException ignored) {
            }

            mTestLinearGrad = new LinearGradient(
                    0, 0, 128, 0,
                    Color.argb(255, 45, 212, 191),
                    Color.argb(255, 14, 165, 233),
                    Shader.TileMode.MIRROR,
                    null
            );
            mTestAngularGrad = new AngularGradient.Builder(
                    0, 0, 0, 360,
                    Shader.TileMode.CLAMP, null, 5)
                    .addColor(0.2f, 0.85f, 0.95f, 1)
                    .addColor(0.85f, 0.5f, 0.75f, 1)
                    .addColor(0.95f, 0.5f, 0.05f, 1)
                    .addColor(0.75f, 0.95f, 0.7f, 1)
                    .addColor(0.6f, 0.25f, 0.65f, 1)
                    .setInterpolationColorSpace(GradientShader.InterpolationColorSpace.SRGB_LINEAR)
                    .build();

            for (int i = 0; i < 13; i++) {
                View v;
                LayoutParams p;
                if (i == 1) {
                    Button button = new Button(getContext());
                    button.setText("Play A Music!");
                    button.setTextColor(0xFF28A3F3);
                    button.setTextStyle(Typeface.BOLD);
                    button.setOnClickListener(__ -> {
                        if (mGoodAnim != null) {
                            mGoodAnim.start();
                        }
                    });
                    /*button.setOnClickListener(v1 -> {
                        String path;
                        try (MemoryStack stack = MemoryStack.stackPush()) {
                            PointerBuffer filters = stack.mallocPointer(1);
                            stack.nUTF8("*.ogg", true);
                            filters.put(stack.getPointerAddress());
                            filters.rewind();
                            path = TinyFileDialogs.tinyfd_openFileDialog(null, null,
                                    filters, "Ogg Vorbis (*.ogg)", false);
                        }
                        if (path != null) {
                            v1.setClickable(false);
                            CompletableFuture.runAsync(() -> {
                                try (FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.READ)) {
                                    ByteBuffer nativeEncodedData = Core.readIntoNativeBuffer(channel).flip();
                                    VorbisPullDecoder decoder = new VorbisPullDecoder(nativeEncodedData);
                                    Track track = new Track(decoder);
                                    sSpectrumGraph = new SpectrumGraph(track, TestLinearLayout.this, false, 600);
                                    track.play();

                                    if (v1.isAttachedToWindow()) {
                                        v1.post(() -> {
                                            v1.invalidate();
                                            if (mGoodAnim != null) {
                                                mGoodAnim.start();
                                            }
                                        });
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    });*/
                    v = button;
                    p = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                } else if (i == 4) {
                    SwitchButton switchButton = new SwitchButton(getContext());
                    v = switchButton;
                    switchButton.setOnCheckedChangeListener((button, checked) -> {
                        if (checked) {
                            button.post(() -> addView(mTextView, 2));
                        } else {
                            button.post(() -> removeView(mTextView));
                        }
                        /*if (checked) {
                            mPopupWindow.setContentView(mTextView);
                            mPopupWindow.setWidth(400);
                            mPopupWindow.setHeight(200);
                            mPopupWindow.setOutsideTouchable(true);
                            mPopupWindow.showAsDropDown(button, 0, 0);
                        } else {
                            mPopupWindow.dismiss();
                        }*/
                    });
                    p = new LayoutParams(dp(50), dp(18));
                } else if (i == 2) {
                    continue;
                } else if (i == 3) {
                    EditText textField = new EditText(getContext());
                    v = textField;
                    p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    textField.setHint("Your Name");
                    textField.setTextSize(16);
                    textField.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    //textField.setSingleLine();
                    //textField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    textField.setPadding(dp(12), 0, dp(12), 0);
                } else if (i == 10) {
                    var layout = new LinearLayout(getContext());
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setHorizontalGravity(Gravity.START);

                    final int dp3 = dp(3);
                    final int dp6 = dp(6);
                    {
                        var title = new TextView(getContext());
                        title.setText("Title");
                        title.setTextSize(14);
                        title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                        var params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
                        params.gravity = Gravity.START;
                        layout.addView(title, params);
                    }
                    {
                        var input = new EditText(getContext());
                        input.setId(R.id.input);
                        input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                        input.setTextSize(14);
                        input.setPadding(dp3, 0, dp3, 0);
                        input.setText("Value");

                        var params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                        params.gravity = Gravity.CENTER_VERTICAL;
                        layout.addView(input, params);
                    }

                    var params = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    params.setMargins(dp6, 0, dp6, 0);
                    v = layout;
                    p = params;
                } else if (i == 5) {
                    RadioGroup group = new RadioGroup(getContext());
                    v = group;
                    for (int j = 0; j < 3; j++) {
                        RadioButton button = new MaterialRadioButton(getContext());
                        button.setText("Item " + j);
                        button.setTextSize(16);
                        button.setId(9 + j);
                        group.addView(button);
                    }
                    group.setOnCheckedChangeListener((__, checkedId) ->
                            Toast.makeText(context, "You checked " + checkedId, Toast.LENGTH_SHORT)
                                    .show());
                    p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                } else if (i == 6) {
                    CheckBox checkBox = new MaterialCheckBox(getContext());
                    v = checkBox;
                    checkBox.setText("Checkbox 0");
                    checkBox.setTextSize(16);
                    checkBox.setGravity(Gravity.END);
                    checkBox.setTooltipText("Hello, this is a tooltip.");
                    p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                } else if (i == 7) {
                    Spinner spinner = new Spinner(getContext());
                    v = spinner;
                    ArrayList<String> list = new ArrayList<>(FontFamily.getSystemFontMap().keySet());
                    list.sort(null);
                    spinner.setAdapter(new ArrayAdapter<>(getContext(), list));
                    p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    spinner.setMinimumWidth(dp(240));
                } else if (i == 11) {
                    var seekbar = new SeekBar(getContext());
                    seekbar.setMax(20);
                    v = seekbar;
                    p = new LayoutParams(dp(200), WRAP_CONTENT);
                } else {
                    v = new CView(getContext(), i);
                    p = new LayoutParams(dp(200), dp(50));
                }
                if (i == 8) {
                    v.setOnCreateContextMenuListener((menu, v1, menuInfo) -> {
                        menu.setQwertyMode(true);
                        menu.setGroupDividerEnabled(true);
                        MenuItem item;
                        item = menu.add(2, Menu.NONE, Menu.NONE, "Align start");
                        item.setAlphabeticShortcut('s', KeyEvent.META_CTRL_ON);
                        item.setChecked(true);
                        item = menu.add(2, Menu.NONE, Menu.NONE, "Align center");
                        item.setAlphabeticShortcut('d', KeyEvent.META_CTRL_ON);
                        item = menu.add(2, Menu.NONE, Menu.NONE, "Align end");
                        item.setAlphabeticShortcut('f', KeyEvent.META_CTRL_ON);
                        menu.setGroupCheckable(2, true, true);
                        SubMenu subMenu = menu.addSubMenu("New");
                        subMenu.add("Document");
                        subMenu.add("Image");
                        menu.add(1, Menu.NONE, Menu.NONE, "Delete");
                    });
                }
                v.setClickable(true);
                p.gravity = Gravity.CENTER;
                addView(v, p);
            }
            addView(new DView(getContext(), TimeInterpolator.DECELERATE), new LayoutParams(dp(120),
                    dp(40)));

            //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
            /*cAnim = new Animation(200)
                    .applyTo(new Applier(20, 0, () -> this.c, v -> this.c = v)
                            .setInterpolator(TimeInterpolator.DECELERATE)
                    );
            circleAnimation1 = new Animation(600)
                    .applyTo(
                            new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc1, v -> circleAcc1 = v)
                                    .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                    );
            circleAnimation2 = new Animation(600)
                    .applyTo(
                            new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc2, v -> circleAcc2 = v)
                                    .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                    );
            circleAnimation3 = new Animation(600)
                    .applyTo(
                            new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc3, v -> circleAcc3 = v)
                                    .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                    );
            circleAnimation4 = new Animation(600)
                    .applyTo(
                            new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc4, v -> circleAcc4 = v)
                                    .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                    );
            iconRadiusAni = new Animation(300)
                    .applyTo(new Applier(40, 80, () -> iconRadius, v -> iconRadius = v)
                            .setInterpolator(TimeInterpolator.DECELERATE)
                    );
            arcStartAni = new Animation(800)
                    .applyTo(new Applier(-90, 270, () -> arcStart, v -> {
                                arcStart = v;
                                invalidate();
                            })
                                    .setInterpolator(TimeInterpolator.DECELERATE)
                    );
            arcEndAni = new Animation(800)
                    .applyTo(new Applier(-90, 270, () -> arcEnd, v -> arcEnd = v)
                            .setInterpolator(TimeInterpolator.ACCELERATE)
                    );*/

            anim = ObjectAnimator.ofFloat(this, sRoundRectLengthProp, 0, 80);
            anim.setDuration(400);
            anim.setInterpolator(TimeInterpolator.OVERSHOOT);
            anim.addUpdateListener(a -> invalidate());
            mRoundRectLenAnim = anim;

            /*ObjectAnimator anim1 = ObjectAnimator.ofFloat(this, sSmoothRadiusProp, 2, 60);
            anim1.setDuration(1000);
            anim1.setRepeatCount(ValueAnimator.INFINITE);
            anim1.setRepeatMode(ValueAnimator.REVERSE);
            anim1.addUpdateListener(a -> invalidate());
            anim1.start();*/

            /*roundRectAlphaAni = new Animation(250)
                    .applyTo(new Applier(0, 1, () -> roundRectAlpha, v -> roundRectAlpha = v));*/

            setLayoutTransition(new LayoutTransition());
        }

        private static final FloatProperty<TestLinearLayout> sRoundRectLengthProp = new FloatProperty<>("roundRectLen"
        ) {
            @Override
            public void setValue(@Nonnull TestLinearLayout object, float value) {
                object.mRoundRectLen = value;
            }

            @Override
            public Float get(@Nonnull TestLinearLayout object) {
                return object.mRoundRectLen;
            }
        };

        private static final FloatProperty<TestLinearLayout> sSmoothRadiusProp = new FloatProperty<>("smoothRadius") {
            @Override
            public void setValue(@Nonnull TestLinearLayout object, float value) {
                object.mSmoothRadius = value;
            }

            @Override
            public Float get(@Nonnull TestLinearLayout object) {
                return object.mSmoothRadius;
            }
        };

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            super.onDraw(canvas);
            /*canvas.moveTo(this);
            canvas.resetColor();
            canvas.setTextAlign(TextAlign.LEFT);
            canvas.save();
            canvas.scale(3, 3);
            canvas.drawText("A Text", 10, 0);
            canvas.drawText(ChatFormatting.BOLD + "A Text", 10, 10);
            canvas.drawText("\u0054\u0068\u0069\u0073\u0020\u0069\u0073\u0020\u0627\u0644\u0644\u063a\u0629\u0020" +
                    "\u0627\u0644\u0639\u0631\u0628\u064a\u0629\u002c\u0020\u0061\u006e\u0064\u0020" +
                    "\u0073\u0068\u0065\u0020\u0069\u0073\u0020\u6d77\u87ba", 10, 20);
            canvas.restore();*/

            Paint paint = Paint.obtain();
            paint.setColor(SystemTheme.COLOR_CONTROL_ACTIVATED);
            paint.setStyle(Paint.FILL);
            canvas.drawRoundRect(6, 90, 46, 104, 7, paint);

            paint.setStyle(Paint.STROKE);
            paint.setStrokeWidth(4.0f);
            canvas.save();
            canvas.rotate(-45);
            canvas.drawRoundRect(6, 110, 86, 124, 6, paint);

            paint.setStyle(Paint.FILL);
            canvas.drawRect(6, 126, 86, 156, paint);
            canvas.restore();

            canvas.drawLine(560, 20, 600, 100, 10, paint);

            canvas.drawLineListMesh(sLinePoints, sLineColors, paint);
            //canvas.drawPointListMesh(sLinePoints, sLineColors, paint);
            canvas.drawTriangleListMesh(sTrianglePoints, sTriangleColors, paint);

            //canvas.drawRoundImage(ICON, 6, 160, 166, 320, iconRadius, paint);

            paint.setStyle(Paint.STROKE);
            canvas.drawPie(100, 200, 50, 60, 120, paint);
            float s1 = (float) Math.sin(AnimationUtils.currentAnimationTimeMillis() / 300D);
            canvas.drawPie(350, 94, 55, 180 + 20 * s1, 100 + 50 * s1 * s1, paint);

            paint.setStrokeWidth(10.0f);
            canvas.drawRect(200, 300, 500, 400, paint);
            paint.setStrokeCap(Paint.CAP_SQUARE);
            canvas.drawRect(200, 450, 500, 550, paint);

            canvas.save();
            canvas.translate(400, 100);
            paint.setShader(mTestImageShader);
            paint.setStyle(Paint.FILL);
            canvas.drawRoundRect(-48, 0, 144, 192, 96, paint);
            canvas.translate(0, 200);
            paint.setShader(mTestLinearGrad);
            paint.setDither(true);
            canvas.drawRoundRect(-192, 0, 192, 96, 32, paint);
            canvas.translate(-200, 200);
            paint.setShader(mTestAngularGrad);
            canvas.drawRoundRect(-100, -100, 100, 100, 25, paint);
            paint.setDither(false);
            paint.setStyle(Paint.STROKE);
            paint.setShader(null);
            canvas.restore();

            paint.setStrokeWidth(40.0f);
            //paint.setSmoothWidth(40.0f);
            //canvas.drawArc(80, 400, 60, arcStart, arcStart - arcEnd, paint);
            paint.setStrokeCap(Paint.CAP_BUTT);
            canvas.drawArc(80, 400, 50, 60, 240, paint);
            canvas.drawBezier(80, 400, 180, 420, 80, 600, paint);

            paint.setStyle(Paint.FILL);
            canvas.drawCircle(80, 700, 60, paint);

            //paint.setSmoothWidth(0.0f);

            paint.setStyle(Paint.FILL);
            paint.setAlpha((int) (roundRectAlpha * 192));
            canvas.drawRoundRect(20, 480, 20 + mRoundRectLen * 1.6f, 480 + mRoundRectLen, 10, paint);
            paint.setAlpha(255);

            paint.recycle();

            // 1

            /*canvas.save();
            RenderSystem.depthMask(true);

            //canvas.scale(f, f, getLeft() + 10, getTop() + 10);
            RenderSystem.translatef(0, 0, 0.001f);
            RenderSystem.colorMask(false, false, false, true);
            //canvas.setColor(0, 0, 0, 128);

            paint.setStyle(Paint.Style.FILL);
            paint.setSmoothRadius(0);
            canvas.drawRoundRect(c, c, 40 - c, 40 - c, 3, paint);

            RenderSystem.translatef(0, 0, -0.001f);
            RenderSystem.colorMask(true, true, true, true);

            paint.setSmoothRadius(1);
            paint.setRGBA(80, 210, 240, 128);
            canvas.drawRoundRect(0, 0, 40, 40, 6, paint);

            canvas.restore();
            RenderSystem.depthMask(false);*/


            // 4

            /*paint.reset();

            canvas.save();
            canvas.translate((float) Math.sin(circleAcc1) * 8, (float) Math.cos(circleAcc1) * 8);
            canvas.drawCircle(40, 18, 3, paint);
            canvas.restore();

            canvas.save();
            canvas.translate((float) Math.sin(circleAcc2) * 8, (float) Math.cos(circleAcc2) * 8);
            canvas.drawCircle(40, 18, 2.5f, paint);
            canvas.restore();

            canvas.save();
            canvas.translate((float) Math.sin(circleAcc3) * 8, (float) Math.cos(circleAcc3) * 8);
            canvas.drawCircle(40, 18, 2, paint);
            canvas.restore();

            canvas.save();
            canvas.translate((float) Math.sin(circleAcc4) * 8, (float) Math.cos(circleAcc4) * 8);
            canvas.drawCircle(40, 18, 1.5f, paint);
            canvas.restore();*/


            // 5

            /*canvas.drawRect(35, 55, 45, 65);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11
            .GL_ONE_MINUS_DST_ALPHA, GL11.GL_ZERO);
            canvas.drawCircle(40, 60, 4);
            RenderSystem.defaultBlendFunc();*/

            // 2
            /*GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

            GL11.glStencilMask(0xff);

            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

            canvas.setColor(255, 255, 255, 128);
            canvas.drawRect(5, 2, 15, 8);

            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xff);

            canvas.setColor(0, 0, 0, 128);
            canvas.drawRect(0, 0, 20, 10);

            GL11.glDisable(GL11.GL_STENCIL_TEST);*/
        }

        protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
            /*if (!b) {
                cAnim.start();
                b = true;
            } else {
                cAnim.invert();
                b = false;
            }
            f = 0.95f;*/
            return true;
        }

        protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
            f = 1;
            return true;
        }

        public void tick() {
            /*ticks++;
            if ((ticks & 15) == 0) {
                if (!b) {
                    cAnim.start();
                    iconRadiusAni.start();
                    b = true;
                } else {
                    cAnim.invert();
                    iconRadiusAni.invert();
                    b = false;
                }
            }
            int a = ticks % 20;
            if (a == 1) {
                circleAnimation1.startFull();
                arcStartAni.startFull();
                arcEndAni.startFull();
                mRoundRectLenAnim.start();
                roundRectAlphaAni.startFull();
            } else if (a == 3) {
                circleAnimation2.startFull();
            } else if (a == 5) {
                circleAnimation3.startFull();
            } else if (a == 7) {
                circleAnimation4.startFull();
            }*/
        }

        private static class CView extends View {

            private final String mIndex;
            TextPaint mTextPaint = new TextPaint();

            public CView(Context context, int index) {
                super(context);
                mIndex = Integer.toString(index);
            }

            @Override
            protected void onDraw(@Nonnull Canvas canvas) {
                if (isHovered()) {
                    Paint paint = Paint.obtain();
                    paint.setARGB(128, 140, 200, 240);
                    canvas.drawRoundRect(0, 1, getWidth(), getHeight() - 2, 4, paint);
                    TextUtils.drawTextRun(canvas, mIndex, 0, mIndex.length(), 0, mIndex.length(), 20,
                            getHeight() >> 1, false,
                            mTextPaint);
                    paint.recycle();
                }
            }

            @Override
            public void onHoverChanged(boolean hovered) {
                super.onHoverChanged(hovered);
                invalidate();
            }
        }

        private static class DView extends View {

            //private final Animation animation;

            private float offsetY;

            private final TextPaint mTextPaint = new TextPaint();
            private int mTicks;

            private final ObjectAnimator mAnimator;

            public DView(Context context, TimeInterpolator interpolator) {
                super(context);
                mTextPaint.setTextSize(13);
                /*animation = new Animation(200)
                        .applyTo(new Applier(0, 60, () -> offsetY, v -> {
                            offsetY = v;
                            invalidate();
                        }).setInterpolator(interpolator));
                animation.invertFull();*/
                PropertyValuesHolder pvh1 = PropertyValuesHolder.ofFloat(ROTATION, 0, 360);
                PropertyValuesHolder pvh2 = PropertyValuesHolder.ofFloat(SCALE_X, 1, 0.2f);
                PropertyValuesHolder pvh3 = PropertyValuesHolder.ofFloat(SCALE_Y, 1, 0.2f);
                PropertyValuesHolder pvh4 = PropertyValuesHolder.ofFloat(TRANSLATION_X, 0, 60);
                PropertyValuesHolder pvh5 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, 0, -180);
                PropertyValuesHolder pvh6 = PropertyValuesHolder.ofFloat(ALPHA, 1, 0);
                mAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvh1, pvh2, pvh3, pvh4, pvh5, pvh6);
                mAnimator.setRepeatCount(1);
                mAnimator.setRepeatMode(ObjectAnimator.REVERSE);
                mAnimator.setDuration(3000);
                setClickable(true);
            }

            @Override
            protected void onDraw(@Nonnull Canvas canvas) {
                Paint paint = Paint.obtain();
                paint.setARGB(128, 140, 200, 240);
                canvas.drawRoundRect(0, 1, getWidth(), getHeight() - 2, 4, paint);
                int x = getWidth() / 2 - 20;
                TextUtils.drawTextRun(canvas, "18:52 modernui", 0, 14, 0, 14, x, offsetY + 24,
                        false, mTextPaint);
                paint.recycle();
            }

            @Override
            public boolean performClick() {
                mAnimator.start();
                return super.performClick();
            }

            /*public void tick() {
                mTicks++;
                if (mTicks % 40 == 0) {
                    animation.invert();
                } else if (mTicks % 20 == 0) {
                    animation.start();
                }
            }*/
        }
    }

    public static class SpectrumGraph {

        private final boolean mCircular;

        private final float[] mAmplitudes = new float[60];
        private final FFT mFFT;
        private final int mHeight;

        private boolean mUpdated;

        public SpectrumGraph(Track track, View view, boolean circular, int height) {
            mFFT = FFT.create(1024, track.getSampleRate());
            mFFT.setLogAverages(250, 14);
            mFFT.setWindowFunc(FFT.HANN);
            track.setAnalyzer(mFFT, f -> {
                updateAmplitudes();
                view.postInvalidate();
            });
            mCircular = circular;
            mHeight = height;
        }

        public void updateAmplitudes() {
            int len = Math.min(mFFT.getAverageSize() - 5, mAmplitudes.length);
            long time = Core.timeMillis();
            int iOff;
            if (mCircular)
                iOff = (int) (time / 200);
            else
                iOff = 0;
            synchronized (mAmplitudes) {
                for (int i = 0; i < len; i++) {
                    float va = mFFT.getAverage(((i + iOff) % len) + 5) / mFFT.getBandSize();
                    mAmplitudes[i] = Math.max(mAmplitudes[i], va);
                }
                mUpdated = true;
            }
        }

        public boolean update(long delta) {
            int len = Math.min(mFFT.getAverageSize() - 5, mAmplitudes.length);
            synchronized (mAmplitudes) {
                for (int i = 0; i < len; i++) {
                    // 2.5e-5f * BPM
                    mAmplitudes[i] = Math.max(0, mAmplitudes[i] - delta * 2.5e-5f * 198f * (mAmplitudes[i] + 0.03f));
                }
                boolean updated = mUpdated;
                mUpdated = false;
                if (!updated) {
                    for (int i = 0; i < len; i++) {
                        if (mAmplitudes[i] > 0) {
                            return true;
                        }
                    }
                }
                return updated;
            }
        }

        public void draw(@Nonnull Canvas canvas, float cx, float cy) {
            var paint = Paint.obtain();
            if (mCircular) {
                long time = Core.timeMillis();
                float b = 1.5f + MathUtil.sin(time / 600f) / 2;
                paint.setRGBA(160, 155, 230, (int) (64 * b));
                paint.setStrokeWidth(200);
                //paint.setSmoothWidth(200);
                paint.setStyle(Paint.STROKE);
                canvas.drawCircle(cx, cy, 130, paint);
                paint.reset();
                for (int i = 0; i < mAmplitudes.length; i++) {
                    float f = Math.abs((i + (int) (time / 100)) % mAmplitudes.length - (mAmplitudes.length - 1) / 2f)
                            / (mAmplitudes.length - 1) * b;
                    paint.setRGBA(100 + (int) (f * 120), 220 - (int) (f * 130), 240 - (int) (f * 20), 255);
                    canvas.rotate(-360f / mAmplitudes.length, cx, cy);
                    canvas.drawRect(cx - 6, cy - 120 - mAmplitudes[i] * mHeight, cx + 6, cy - 120, paint);
                }
            } else {
                for (int i = 0; i < mAmplitudes.length; i++) {
                    paint.setRGBA(100 + i * 2, 220 - i * 2, 240 - i * 4, 255);
                    canvas.drawRect(cx - 479 + i * 16, cy - mAmplitudes[i] * mHeight, cx - 465 + i * 16, cy, paint);
                }
            }
            paint.recycle();
        }
    }
}
