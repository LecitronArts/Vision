package net.optifine.shaders.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.optifine.Config;
import net.optifine.expr.ExpressionFloatArrayCached;
import net.optifine.expr.ExpressionFloatCached;
import net.optifine.expr.ExpressionParser;
import net.optifine.expr.ExpressionType;
import net.optifine.expr.IExpression;
import net.optifine.expr.IExpressionBool;
import net.optifine.expr.IExpressionFloat;
import net.optifine.expr.IExpressionFloatArray;
import net.optifine.expr.ParseException;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Program;
import net.optifine.shaders.SMCLog;
import net.optifine.shaders.ShaderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersCompatibility;
import net.optifine.shaders.uniform.CustomUniform;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderExpressionResolver;
import net.optifine.shaders.uniform.UniformType;
import net.optifine.util.DynamicDimension;
import net.optifine.util.LineBuffer;
import net.optifine.util.StrUtils;

public class ShaderPackParser {
   public static final Pattern PATTERN_VERSION = Pattern.compile("^\\s*#version\\s+(\\d+).*$");
   public static final Pattern PATTERN_INCLUDE = Pattern.compile("^\\s*#include\\s+\"([A-Za-z0-9_/\\.]+)\".*$");
   private static final Set<String> setConstNames = makeSetConstNames();
   private static final Map<String, Integer> mapAlphaFuncs = makeMapAlphaFuncs();
   private static final Map<String, Integer> mapBlendFactors = makeMapBlendFactors();

   public static ShaderOption[] parseShaderPackOptions(IShaderPack shaderPack, String[] programNames, List<Integer> listDimensions) {
      if (shaderPack == null) {
         return new ShaderOption[0];
      } else {
         Map<String, ShaderOption> map = new HashMap<>();
         collectShaderOptions(shaderPack, "/shaders", programNames, map);

         for(int i : listDimensions) {
            String s = "/shaders/world" + i;
            collectShaderOptions(shaderPack, s, programNames, map);
         }

         Collection<ShaderOption> collection = map.values();
         ShaderOption[] ashaderoption = collection.toArray(new ShaderOption[collection.size()]);
         Comparator<ShaderOption> comparator = new Comparator<ShaderOption>() {
            public int compare(ShaderOption o1, ShaderOption o2) {
               return o1.getName().compareToIgnoreCase(o2.getName());
            }
         };
         Arrays.sort(ashaderoption, comparator);
         return ashaderoption;
      }
   }

   private static void collectShaderOptions(IShaderPack shaderPack, String dir, String[] programNames, Map<String, ShaderOption> mapOptions) {
      for(int i = 0; i < programNames.length; ++i) {
         String s = programNames[i];
         if (!s.equals("")) {
            String s1 = dir + "/" + s + ".csh";
            String s2 = dir + "/" + s + ".vsh";
            String s3 = dir + "/" + s + ".gsh";
            String s4 = dir + "/" + s + ".fsh";
            collectShaderOptions(shaderPack, s1, mapOptions);
            collectShaderOptions(shaderPack, s2, mapOptions);
            collectShaderOptions(shaderPack, s3, mapOptions);
            collectShaderOptions(shaderPack, s4, mapOptions);
         }
      }

   }

   private static void collectShaderOptions(IShaderPack sp, String path, Map<String, ShaderOption> mapOptions) {
      String[] astring = getLines(sp, path);

      for(int i = 0; i < astring.length; ++i) {
         String s = astring[i];
         ShaderOption shaderoption = getShaderOption(s, path);
         if (shaderoption != null && !shaderoption.getName().startsWith(ShaderMacros.getPrefixMacro())) {
            String s1 = shaderoption.getName();
            ShaderOption shaderoption1 = mapOptions.get(s1);
            if (shaderoption1 != null) {
               if (!Config.equals(shaderoption1.getValueDefault(), shaderoption.getValueDefault())) {
                  if (shaderoption1.isEnabled()) {
                     Config.warn("Ambiguous shader option: " + shaderoption.getName());
                     Config.warn(" - in " + Config.arrayToString((Object[])shaderoption1.getPaths()) + ": " + shaderoption1.getValueDefault());
                     Config.warn(" - in " + Config.arrayToString((Object[])shaderoption.getPaths()) + ": " + shaderoption.getValueDefault());
                  }

                  shaderoption1.setEnabled(false);
               }

               if (shaderoption1.getDescription() == null || shaderoption1.getDescription().length() <= 0) {
                  shaderoption1.setDescription(shaderoption.getDescription());
               }

               shaderoption1.addPaths(shaderoption.getPaths());
            } else if (!shaderoption.checkUsed() || isOptionUsed(shaderoption, astring)) {
               mapOptions.put(s1, shaderoption);
            }
         }
      }

   }

   private static boolean isOptionUsed(ShaderOption so, String[] lines) {
      for(int i = 0; i < lines.length; ++i) {
         String s = lines[i];
         if (so.isUsedInLine(s)) {
            return true;
         }
      }

      return false;
   }

   private static String[] getLines(IShaderPack sp, String path) {
      try {
         List<String> list = new ArrayList<>();
         LineBuffer linebuffer = loadFile(path, sp, 0, list, 0);
         return linebuffer == null ? new String[0] : linebuffer.getLines();
      } catch (IOException ioexception) {
         Config.dbg(ioexception.getClass().getName() + ": " + ioexception.getMessage());
         return new String[0];
      }
   }

   private static ShaderOption getShaderOption(String line, String path) {
      ShaderOption shaderoption = null;
      if (shaderoption == null) {
         shaderoption = ShaderOptionSwitch.parseOption(line, path);
      }

      if (shaderoption == null) {
         shaderoption = ShaderOptionVariable.parseOption(line, path);
      }

      if (shaderoption != null) {
         return shaderoption;
      } else {
         if (shaderoption == null) {
            shaderoption = ShaderOptionSwitchConst.parseOption(line, path);
         }

         if (shaderoption == null) {
            shaderoption = ShaderOptionVariableConst.parseOption(line, path);
         }

         return shaderoption != null && setConstNames.contains(shaderoption.getName()) ? shaderoption : null;
      }
   }

   private static Set<String> makeSetConstNames() {
      Set<String> set = new HashSet<>();
      set.add("shadowMapResolution");
      set.add("shadowMapFov");
      set.add("shadowDistance");
      set.add("shadowDistanceRenderMul");
      set.add("shadowIntervalSize");
      set.add("generateShadowMipmap");
      set.add("generateShadowColorMipmap");
      set.add("shadowHardwareFiltering");
      set.add("shadowHardwareFiltering0");
      set.add("shadowHardwareFiltering1");
      set.add("shadowtex0Mipmap");
      set.add("shadowtexMipmap");
      set.add("shadowtex1Mipmap");
      set.add("shadowcolor0Mipmap");
      set.add("shadowColor0Mipmap");
      set.add("shadowcolor1Mipmap");
      set.add("shadowColor1Mipmap");
      set.add("shadowtex0Nearest");
      set.add("shadowtexNearest");
      set.add("shadow0MinMagNearest");
      set.add("shadowtex1Nearest");
      set.add("shadow1MinMagNearest");
      set.add("shadowcolor0Nearest");
      set.add("shadowColor0Nearest");
      set.add("shadowColor0MinMagNearest");
      set.add("shadowcolor1Nearest");
      set.add("shadowColor1Nearest");
      set.add("shadowColor1MinMagNearest");
      set.add("wetnessHalflife");
      set.add("drynessHalflife");
      set.add("eyeBrightnessHalflife");
      set.add("centerDepthHalflife");
      set.add("sunPathRotation");
      set.add("ambientOcclusionLevel");
      set.add("superSamplingLevel");
      set.add("noiseTextureResolution");
      return set;
   }

   public static ShaderProfile[] parseProfiles(Properties props, ShaderOption[] shaderOptions) {
      String s = "profile.";
      List<ShaderProfile> list = new ArrayList<>();

      for(String s1 : (Set<String>)(Set<?>) props.keySet()) {
         if (s1.startsWith(s)) {
            String s2 = s1.substring(s.length());
            String s3 = props.getProperty(s1);
            Set<String> set = new HashSet<>();
            ShaderProfile shaderprofile = parseProfile(s2, props, set, shaderOptions);
            if (shaderprofile != null) {
               list.add(shaderprofile);
            }
         }
      }

      if (list.size() <= 0) {
         return null;
      } else {
         ShaderProfile[] ashaderprofile = list.toArray(new ShaderProfile[list.size()]);
         return ashaderprofile;
      }
   }

   public static Map<String, IExpressionBool> parseProgramConditions(Properties props, ShaderOption[] shaderOptions) {
      String s = "program.";
      Pattern pattern = Pattern.compile("program\\.([^.]+)\\.enabled");
      Map<String, IExpressionBool> map = new HashMap<>();

      for(String s1 : (Set<String>)(Set<?>) props.keySet()) {
         Matcher matcher = pattern.matcher(s1);
         if (matcher.matches()) {
            String s2 = matcher.group(1);
            String s3 = props.getProperty(s1).trim();
            IExpressionBool iexpressionbool = parseOptionExpression(s3, shaderOptions);
            if (iexpressionbool == null) {
               SMCLog.severe("Error parsing program condition: " + s1);
            } else {
               map.put(s2, iexpressionbool);
            }
         }
      }

      return map;
   }

   private static IExpressionBool parseOptionExpression(String val, ShaderOption[] shaderOptions) {
      try {
         ShaderOptionResolver shaderoptionresolver = new ShaderOptionResolver(shaderOptions);
         ExpressionParser expressionparser = new ExpressionParser(shaderoptionresolver);
         return expressionparser.parseBool(val);
      } catch (ParseException parseexception) {
         SMCLog.warning(parseexception.getClass().getName() + ": " + parseexception.getMessage());
         return null;
      }
   }

   public static Set<String> parseOptionSliders(Properties props, ShaderOption[] shaderOptions) {
      Set<String> set = new HashSet<>();
      String s = props.getProperty("sliders");
      if (s == null) {
         return set;
      } else {
         String[] astring = Config.tokenize(s, " ");

         for(int i = 0; i < astring.length; ++i) {
            String s1 = astring[i];
            ShaderOption shaderoption = ShaderUtils.getShaderOption(s1, shaderOptions);
            if (shaderoption == null) {
               Config.warn("Invalid shader option: " + s1);
            } else {
               set.add(s1);
            }
         }

         return set;
      }
   }

   private static ShaderProfile parseProfile(String name, Properties props, Set<String> parsedProfiles, ShaderOption[] shaderOptions) {
      String s = "profile.";
      String s1 = s + name;
      if (parsedProfiles.contains(s1)) {
         Config.warn("[Shaders] Profile already parsed: " + name);
         return null;
      } else {
         parsedProfiles.add(name);
         ShaderProfile shaderprofile = new ShaderProfile(name);
         String s2 = props.getProperty(s1);
         String[] astring = Config.tokenize(s2, " ");

         for(int i = 0; i < astring.length; ++i) {
            String s3 = astring[i];
            if (s3.startsWith(s)) {
               String s4 = s3.substring(s.length());
               ShaderProfile shaderprofile1 = parseProfile(s4, props, parsedProfiles, shaderOptions);
               if (shaderprofile != null) {
                  shaderprofile.addOptionValues(shaderprofile1);
                  shaderprofile.addDisabledPrograms(shaderprofile1.getDisabledPrograms());
               }
            } else {
               String[] astring1 = Config.tokenize(s3, ":=");
               if (astring1.length == 1) {
                  String s7 = astring1[0];
                  boolean flag = true;
                  if (s7.startsWith("!")) {
                     flag = false;
                     s7 = s7.substring(1);
                  }

                  String s5 = "program.";
                  if (s7.startsWith(s5)) {
                     String s6 = s7.substring(s5.length());
                     if (!Shaders.isProgramPath(s6)) {
                        Config.warn("Invalid program: " + s6 + " in profile: " + shaderprofile.getName());
                     } else if (flag) {
                        shaderprofile.removeDisabledProgram(s6);
                     } else {
                        shaderprofile.addDisabledProgram(s6);
                     }
                  } else {
                     ShaderOption shaderoption1 = ShaderUtils.getShaderOption(s7, shaderOptions);
                     if (!(shaderoption1 instanceof ShaderOptionSwitch)) {
                        Config.warn("[Shaders] Invalid option: " + s7);
                     } else {
                        shaderprofile.addOptionValue(s7, String.valueOf(flag));
                        shaderoption1.setVisible(true);
                     }
                  }
               } else if (astring1.length != 2) {
                  Config.warn("[Shaders] Invalid option value: " + s3);
               } else {
                  String s8 = astring1[0];
                  String s9 = astring1[1];
                  ShaderOption shaderoption = ShaderUtils.getShaderOption(s8, shaderOptions);
                  if (shaderoption == null) {
                     Config.warn("[Shaders] Invalid option: " + s3);
                  } else if (!shaderoption.isValidValue(s9)) {
                     Config.warn("[Shaders] Invalid value: " + s3);
                  } else {
                     shaderoption.setVisible(true);
                     shaderprofile.addOptionValue(s8, s9);
                  }
               }
            }
         }

         return shaderprofile;
      }
   }

   public static Map<String, ScreenShaderOptions> parseGuiScreens(Properties props, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions) {
      Map<String, ScreenShaderOptions> map = new HashMap<>();
      parseGuiScreen("screen", props, map, shaderProfiles, shaderOptions);
      return map.isEmpty() ? null : map;
   }

   private static boolean parseGuiScreen(String key, Properties props, Map<String, ScreenShaderOptions> map, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions) {
      String s = props.getProperty(key);
      if (s == null) {
         return false;
      } else {
         String s1 = key + "$parent$";
         if (map.containsKey(s1)) {
            Config.warn("[Shaders] Screen circular reference: " + key + " = " + s);
            return false;
         } else {
            List<ShaderOption> list = new ArrayList<>();
            Set<String> set = new HashSet<>();
            String[] astring = Config.tokenize(s, " ");

            for(int i = 0; i < astring.length; ++i) {
               String s2 = astring[i];
               if (s2.equals("<empty>")) {
                  list.add((ShaderOption)null);
               } else if (set.contains(s2)) {
                  Config.warn("[Shaders] Duplicate option: " + s2 + ", key: " + key);
               } else {
                  set.add(s2);
                  if (s2.equals("<profile>")) {
                     if (shaderProfiles == null) {
                        Config.warn("[Shaders] Option profile can not be used, no profiles defined: " + s2 + ", key: " + key);
                     } else {
                        ShaderOptionProfile shaderoptionprofile = new ShaderOptionProfile(shaderProfiles, shaderOptions);
                        list.add(shaderoptionprofile);
                     }
                  } else if (s2.equals("*")) {
                     ShaderOption shaderoption = new ShaderOptionRest("<rest>");
                     list.add(shaderoption);
                  } else if (s2.startsWith("[") && s2.endsWith("]")) {
                     String s4 = StrUtils.removePrefixSuffix(s2, "[", "]");
                     if (!s4.matches("^[a-zA-Z0-9_]+$")) {
                        Config.warn("[Shaders] Invalid screen: " + s2 + ", key: " + key);
                     } else {
                        map.put(s1, (ScreenShaderOptions)null);
                        boolean flag = parseGuiScreen("screen." + s4, props, map, shaderProfiles, shaderOptions);
                        map.remove(s1);
                        if (!flag) {
                           Config.warn("[Shaders] Invalid screen: " + s2 + ", key: " + key);
                        } else {
                           ShaderOptionScreen shaderoptionscreen = new ShaderOptionScreen(s4);
                           list.add(shaderoptionscreen);
                        }
                     }
                  } else {
                     ShaderOption shaderoption1 = ShaderUtils.getShaderOption(s2, shaderOptions);
                     if (shaderoption1 == null) {
                        Config.warn("[Shaders] Invalid option: " + s2 + ", key: " + key);
                        list.add((ShaderOption)null);
                     } else {
                        shaderoption1.setVisible(true);
                        list.add(shaderoption1);
                     }
                  }
               }
            }

            ShaderOption[] ashaderoption = list.toArray(new ShaderOption[list.size()]);
            String s3 = props.getProperty(key + ".columns");
            int j = Config.parseInt(s3, 2);
            ScreenShaderOptions screenshaderoptions = new ScreenShaderOptions(key, ashaderoption, j);
            map.put(key, screenshaderoptions);
            return true;
         }
      }
   }

   public static LineBuffer loadShader(Program program, ShaderType shaderType, InputStream is, String filePath, IShaderPack shaderPack, List<String> listFiles, ShaderOption[] activeOptions) throws IOException {
      LineBuffer linebuffer = LineBuffer.readAll(new InputStreamReader(is));
      linebuffer = resolveIncludes(linebuffer, filePath, shaderPack, 0, listFiles, 0);
      linebuffer = addMacros(linebuffer, 0);
      linebuffer = remapTextureUnits(linebuffer);
      LineBuffer linebuffer1 = new LineBuffer();

      for(String s : linebuffer) {
         s = applyOptions(s, activeOptions);
         linebuffer1.add(s);
      }

      return ShadersCompatibility.remap(program, shaderType, linebuffer1);
   }

   private static String applyOptions(String line, ShaderOption[] ops) {
      if (ops != null && ops.length > 0) {
         for(int i = 0; i < ops.length; ++i) {
            ShaderOption shaderoption = ops[i];
            if (shaderoption.matchesLine(line)) {
               line = shaderoption.getSourceLine();
               break;
            }
         }

         return line;
      } else {
         return line;
      }
   }

   public static LineBuffer resolveIncludes(LineBuffer reader, String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel) throws IOException {
      String s = "/";
      int i = filePath.lastIndexOf("/");
      if (i >= 0) {
         s = filePath.substring(0, i);
      }

      LineBuffer linebuffer = new LineBuffer();
      int j = 0;

      for(String s1 : reader) {
         ++j;
         Matcher matcher = PATTERN_INCLUDE.matcher(s1);
         if (matcher.matches()) {
            String s2 = matcher.group(1);
            boolean flag = s2.startsWith("/");
            String s3 = flag ? "/shaders" + s2 : s + "/" + s2;
            if (!listFiles.contains(s3)) {
               listFiles.add(s3);
            }

            int k = listFiles.indexOf(s3) + 1;
            LineBuffer linebuffer1 = loadFile(s3, shaderPack, k, listFiles, includeLevel);
            if (linebuffer1 == null) {
               throw new IOException("Included file not found: " + filePath);
            }

            if (linebuffer1.indexMatch(PATTERN_VERSION) < 0) {
               linebuffer.add("#line 1 " + k);
            }

            linebuffer.add(linebuffer1.getLines());
            linebuffer.add("#line " + (j + 1) + " " + fileIndex);
         } else {
            linebuffer.add(s1);
         }
      }

      return linebuffer;
   }

   public static LineBuffer addMacros(LineBuffer reader, int fileIndex) throws IOException {
      LineBuffer linebuffer = new LineBuffer(reader.getLines());
      int i = linebuffer.indexMatch(PATTERN_VERSION);
      if (i < 0) {
         Config.warn("Macro insert position not found");
         return reader;
      } else {
         ++i;
         String s = "#line " + (i + 1) + " " + fileIndex;
         String[] astring = ShaderMacros.getHeaderMacroLines();
         linebuffer.insert(i, astring);
         i += astring.length;
         ShaderMacro[] ashadermacro = getCustomMacros(linebuffer, i);
         if (ashadermacro.length > 0) {
            LineBuffer linebuffer1 = new LineBuffer();

            for(int j = 0; j < ashadermacro.length; ++j) {
               ShaderMacro shadermacro = ashadermacro[j];
               linebuffer1.add(shadermacro.getSourceLine());
            }

            linebuffer.insert(i, linebuffer1.getLines());
            i += linebuffer1.size();
         }

         linebuffer.insert(i, s);
         return linebuffer;
      }
   }

   private static ShaderMacro[] getCustomMacros(LineBuffer lines, int startPos) {
      Set<ShaderMacro> set = new LinkedHashSet<>();

      for(int i = startPos; i < lines.size(); ++i) {
         String s = lines.get(i);
         if (s.contains(ShaderMacros.getPrefixMacro())) {
            ShaderMacro[] ashadermacro = findMacros(s, ShaderMacros.getExtensions());
            set.addAll(Arrays.asList(ashadermacro));
            ShaderMacro[] ashadermacro1 = findMacros(s, ShaderMacros.getConstantMacros());
            set.addAll(Arrays.asList(ashadermacro1));
         }
      }

      return set.toArray(new ShaderMacro[set.size()]);
   }

   public static LineBuffer remapTextureUnits(LineBuffer reader) throws IOException {
      if (!Shaders.isRemapLightmap()) {
         return reader;
      } else {
         LineBuffer linebuffer = new LineBuffer();

         for(String s : reader) {
            String s1 = s.replace("gl_TextureMatrix[1]", "gl_TextureMatrix[2]");
            s1 = s1.replace("gl_MultiTexCoord1", "gl_MultiTexCoord2");
            if (!s1.equals(s)) {
               s1 = s1 + " // Legacy fix, replaced TU 1 with 2";
               s = s1;
            }

            linebuffer.add(s);
         }

         return linebuffer;
      }
   }

   private static ShaderMacro[] findMacros(String line, ShaderMacro[] macros) {
      List<ShaderMacro> list = new ArrayList<>();

      for(int i = 0; i < macros.length; ++i) {
         ShaderMacro shadermacro = macros[i];
         if (line.contains(shadermacro.getName())) {
            list.add(shadermacro);
         }
      }

      return list.toArray(new ShaderMacro[list.size()]);
   }

   private static LineBuffer loadFile(String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel) throws IOException {
      if (includeLevel >= 10) {
         throw new IOException("#include depth exceeded: " + includeLevel + ", file: " + filePath);
      } else {
         ++includeLevel;
         InputStream inputstream = shaderPack.getResourceAsStream(filePath);
         if (inputstream == null) {
            return null;
         } else {
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream, "ASCII");
            LineBuffer linebuffer = LineBuffer.readAll(inputstreamreader);
            return resolveIncludes(linebuffer, filePath, shaderPack, fileIndex, listFiles, includeLevel);
         }
      }
   }

   public static CustomUniforms parseCustomUniforms(Properties props) {
      String s = "uniform";
      String s1 = "variable";
      String s2 = s + ".";
      String s3 = s1 + ".";
      Map<String, IExpression> map = new HashMap<>();
      List<CustomUniform> list = new ArrayList<>();

      for(String s4 : (Set<String>)(Set<?>) props.keySet()) {
         String[] astring = Config.tokenize(s4, ".");
         if (astring.length == 3) {
            String s5 = astring[0];
            String s6 = astring[1];
            String s7 = astring[2];
            String s8 = props.getProperty(s4).trim();
            if (map.containsKey(s7)) {
               SMCLog.warning("Expression already defined: " + s7);
            } else if (s5.equals(s) || s5.equals(s1)) {
               SMCLog.info("Custom " + s5 + ": " + s7);
               CustomUniform customuniform = parseCustomUniform(s5, s7, s6, s8, map);
               if (customuniform != null) {
                  map.put(s7, customuniform.getExpression());
                  if (!s5.equals(s1)) {
                     list.add(customuniform);
                  }
               }
            }
         }
      }

      if (list.size() <= 0) {
         return null;
      } else {
         CustomUniform[] acustomuniform = list.toArray(new CustomUniform[list.size()]);
         CustomUniforms customuniforms = new CustomUniforms(acustomuniform, map);
         return customuniforms;
      }
   }

   private static CustomUniform parseCustomUniform(String kind, String name, String type, String src, Map<String, IExpression> mapExpressions) {
      try {
         UniformType uniformtype = UniformType.parse(type);
         if (uniformtype == null) {
            SMCLog.warning("Unknown " + kind + " type: " + uniformtype);
            return null;
         } else {
            ShaderExpressionResolver shaderexpressionresolver = new ShaderExpressionResolver(mapExpressions);
            ExpressionParser expressionparser = new ExpressionParser(shaderexpressionresolver);
            IExpression iexpression = expressionparser.parse(src);
            ExpressionType expressiontype = iexpression.getExpressionType();
            if (!uniformtype.matchesExpressionType(expressiontype)) {
               SMCLog.warning("Expression type does not match " + kind + " type, expression: " + expressiontype + ", " + kind + ": " + uniformtype + " " + name);
               return null;
            } else {
               iexpression = makeExpressionCached(iexpression);
               return new CustomUniform(name, uniformtype, iexpression);
            }
         }
      } catch (ParseException parseexception) {
         SMCLog.warning(parseexception.getClass().getName() + ": " + parseexception.getMessage());
         return null;
      }
   }

   private static IExpression makeExpressionCached(IExpression expr) {
      if (expr instanceof IExpressionFloat) {
         return new ExpressionFloatCached((IExpressionFloat)expr);
      } else {
         return (IExpression)(expr instanceof IExpressionFloatArray ? new ExpressionFloatArrayCached((IExpressionFloatArray)expr) : expr);
      }
   }

   public static void parseAlphaStates(Properties props) {
      for(String s : (Set<String>)(Set<?>) props.keySet()) {
         String[] astring = Config.tokenize(s, ".");
         if (astring.length == 2) {
            String s1 = astring[0];
            String s2 = astring[1];
            if (s1.equals("alphaTest")) {
               Program program = Shaders.getProgram(s2);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + s2);
               } else {
                  String s3 = props.getProperty(s).trim();
                  GlAlphaState glalphastate = parseAlphaState(s3);
                  if (glalphastate != null) {
                     program.setAlphaState(glalphastate);
                  }
               }
            }
         }
      }

   }

   public static GlAlphaState parseAlphaState(String str) {
      if (str == null) {
         return null;
      } else {
         String[] astring = Config.tokenize(str, " ");
         if (astring.length == 1) {
            String s = astring[0];
            if (s.equals("off") || s.equals("false")) {
               return new GlAlphaState(false);
            }
         } else if (astring.length == 2) {
            String s2 = astring[0];
            String s1 = astring[1];
            Integer integer = mapAlphaFuncs.get(s2);
            float f = Config.parseFloat(s1, -1.0F);
            if (integer != null && f >= 0.0F) {
               return new GlAlphaState(true, integer, f);
            }
         }

         SMCLog.severe("Invalid alpha test: " + str);
         return null;
      }
   }

   public static void parseBlendStates(Properties props) {
      for(String s : (Set<String>)(Set<?>) props.keySet()) {
         String[] astring = Config.tokenize(s, ".");
         if (astring.length >= 2 && astring.length <= 3) {
            String s1 = astring[0];
            String s2 = astring[1];
            String s3 = astring.length == 3 ? astring[2] : null;
            if (s1.equals("blend")) {
               Program program = Shaders.getProgram(s2);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + s2);
               } else {
                  String s4 = props.getProperty(s).trim();
                  GlBlendState glblendstate = parseBlendState(s4);
                  if (glblendstate != null) {
                     if (s3 != null) {
                        int i = program.getProgramStage().isAnyShadow() ? ShaderParser.getShadowColorIndex(s3) : Shaders.getBufferIndex(s3);
                        int j = program.getProgramStage().isAnyShadow() ? 2 : 16;
                        if (i >= 0 && i < j) {
                           program.setBlendStateColorIndexed(i, glblendstate);
                           SMCLog.info("Blend " + s2 + "." + s3 + "=" + s4);
                        } else {
                           SMCLog.severe("Invalid buffer name: " + s3);
                        }
                     } else {
                        program.setBlendState(glblendstate);
                     }
                  }
               }
            }
         }
      }

   }

   public static GlBlendState parseBlendState(String str) {
      if (str == null) {
         return null;
      } else {
         String[] astring = Config.tokenize(str, " ");
         if (astring.length == 1) {
            String s = astring[0];
            if (s.equals("off") || s.equals("false")) {
               return new GlBlendState(false);
            }
         } else if (astring.length == 2 || astring.length == 4) {
            String s4 = astring[0];
            String s1 = astring[1];
            String s2 = s4;
            String s3 = s1;
            if (astring.length == 4) {
               s2 = astring[2];
               s3 = astring[3];
            }

            Integer integer = mapBlendFactors.get(s4);
            Integer integer1 = mapBlendFactors.get(s1);
            Integer integer2 = mapBlendFactors.get(s2);
            Integer integer3 = mapBlendFactors.get(s3);
            if (integer != null && integer1 != null && integer2 != null && integer3 != null) {
               return new GlBlendState(true, integer, integer1, integer2, integer3);
            }
         }

         SMCLog.severe("Invalid blend mode: " + str);
         return null;
      }
   }

   public static void parseRenderScales(Properties props) {
      for(String s : (Set<String>)(Set<?>) props.keySet()) {
         String[] astring = Config.tokenize(s, ".");
         if (astring.length == 2) {
            String s1 = astring[0];
            String s2 = astring[1];
            if (s1.equals("scale")) {
               Program program = Shaders.getProgram(s2);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + s2);
               } else {
                  String s3 = props.getProperty(s).trim();
                  RenderScale renderscale = parseRenderScale(s3);
                  if (renderscale != null) {
                     program.setRenderScale(renderscale);
                  }
               }
            }
         }
      }

   }

   private static RenderScale parseRenderScale(String str) {
      if (str == null) {
         return null;
      } else {
         String[] astring = Config.tokenize(str, " ");
         float f = Config.parseFloat(astring[0], -1.0F);
         float f1 = 0.0F;
         float f2 = 0.0F;
         if (astring.length > 1) {
            if (astring.length != 3) {
               SMCLog.severe("Invalid render scale: " + str);
               return null;
            }

            f1 = Config.parseFloat(astring[1], -1.0F);
            f2 = Config.parseFloat(astring[2], -1.0F);
         }

         if (Config.between(f, 0.0F, 1.0F) && Config.between(f1, 0.0F, 1.0F) && Config.between(f2, 0.0F, 1.0F)) {
            return new RenderScale(f, f1, f2);
         } else {
            SMCLog.severe("Invalid render scale: " + str);
            return null;
         }
      }
   }

   public static void parseBuffersFlip(Properties props) {
      for(String s : (Set<String>)(Set<?>) props.keySet()) {
         String[] astring = Config.tokenize(s, ".");
         if (astring.length == 3) {
            String s1 = astring[0];
            String s2 = astring[1];
            String s3 = astring[2];
            if (s1.equals("flip")) {
               Program program = Shaders.getProgram(s2);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + s2);
               } else {
                  Boolean[] aboolean = program.getBuffersFlip();
                  int i = Shaders.getBufferIndex(s3);
                  if (i >= 0 && i < aboolean.length) {
                     String s4 = props.getProperty(s).trim();
                     Boolean obool = Config.parseBoolean(s4, (Boolean)null);
                     if (obool == null) {
                        SMCLog.severe("Invalid boolean value: " + s4);
                     } else {
                        aboolean[i] = obool;
                     }
                  } else {
                     SMCLog.severe("Invalid buffer name: " + s3);
                  }
               }
            }
         }
      }

   }

   private static Map<String, Integer> makeMapAlphaFuncs() {
      Map<String, Integer> map = new HashMap<>();
      map.put("NEVER", new Integer(512));
      map.put("LESS", new Integer(513));
      map.put("EQUAL", new Integer(514));
      map.put("LEQUAL", new Integer(515));
      map.put("GREATER", new Integer(516));
      map.put("NOTEQUAL", new Integer(517));
      map.put("GEQUAL", new Integer(518));
      map.put("ALWAYS", new Integer(519));
      return Collections.unmodifiableMap(map);
   }

   private static Map<String, Integer> makeMapBlendFactors() {
      Map<String, Integer> map = new HashMap<>();
      map.put("ZERO", new Integer(0));
      map.put("ONE", new Integer(1));
      map.put("SRC_COLOR", new Integer(768));
      map.put("ONE_MINUS_SRC_COLOR", new Integer(769));
      map.put("DST_COLOR", new Integer(774));
      map.put("ONE_MINUS_DST_COLOR", new Integer(775));
      map.put("SRC_ALPHA", new Integer(770));
      map.put("ONE_MINUS_SRC_ALPHA", new Integer(771));
      map.put("DST_ALPHA", new Integer(772));
      map.put("ONE_MINUS_DST_ALPHA", new Integer(773));
      map.put("CONSTANT_COLOR", new Integer(32769));
      map.put("ONE_MINUS_CONSTANT_COLOR", new Integer(32770));
      map.put("CONSTANT_ALPHA", new Integer(32771));
      map.put("ONE_MINUS_CONSTANT_ALPHA", new Integer(32772));
      map.put("SRC_ALPHA_SATURATE", new Integer(776));
      return Collections.unmodifiableMap(map);
   }

   public static DynamicDimension[] parseBufferSizes(Properties props, int countBuffers) {
      DynamicDimension[] adynamicdimension = new DynamicDimension[countBuffers];

      for(String s : (Set<String>)(Set<?>) props.keySet()) {
         if (s.startsWith("size.buffer.")) {
            String[] astring = Config.tokenize(s, ".");
            if (astring.length == 3) {
               String s1 = astring[2];
               int i = Shaders.getBufferIndex(s1);
               if (i >= 0 && i < adynamicdimension.length) {
                  String s2 = props.getProperty(s).trim();
                  DynamicDimension dynamicdimension = parseDynamicDimension(s2);
                  if (dynamicdimension == null) {
                     SMCLog.severe("Invalid buffer size: " + s + "=" + s2);
                  } else {
                     adynamicdimension[i] = dynamicdimension;
                     if (dynamicdimension.isRelative()) {
                        SMCLog.info("Relative size " + s1 + ": " + dynamicdimension.getWidth() + " " + dynamicdimension.getHeight());
                     } else {
                        SMCLog.info("Fixed size " + s1 + ": " + (int)dynamicdimension.getWidth() + " " + (int)dynamicdimension.getHeight());
                     }
                  }
               } else {
                  SMCLog.severe("Invalid buffer name: " + s);
               }
            }
         }
      }

      return adynamicdimension;
   }

   private static DynamicDimension parseDynamicDimension(String str) {
      if (str == null) {
         return null;
      } else {
         String[] astring = Config.tokenize(str, " ");
         if (astring.length != 2) {
            return null;
         } else {
            int i = Config.parseInt(astring[0], -1);
            int j = Config.parseInt(astring[1], -1);
            if (i >= 0 && j >= 0) {
               return new DynamicDimension(false, (float)i, (float)j);
            } else {
               float f = Config.parseFloat(astring[0], -1.0F);
               float f1 = Config.parseFloat(astring[1], -1.0F);
               return f >= 0.0F && f1 >= 0.0F ? new DynamicDimension(true, f, f1) : null;
            }
         }
      }
   }
}
