package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class SimpleCookingSerializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {
   private final AbstractCookingRecipe.Factory<T> factory;
   private final Codec<T> codec;

   public SimpleCookingSerializer(AbstractCookingRecipe.Factory<T> pFactory, int pCookingTime) {
      this.factory = pFactory;
      this.codec = RecordCodecBuilder.create((p_296927_) -> {
         return p_296927_.group(ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter((p_296921_) -> {
            return p_296921_.group;
         }), CookingBookCategory.CODEC.fieldOf("category").orElse(CookingBookCategory.MISC).forGetter((p_296924_) -> {
            return p_296924_.category;
         }), Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter((p_296920_) -> {
            return p_296920_.ingredient;
         }), BuiltInRegistries.ITEM.byNameCodec().xmap(ItemStack::new, ItemStack::getItem).fieldOf("result").forGetter((p_296923_) -> {
            return p_296923_.result;
         }), Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter((p_296922_) -> {
            return p_296922_.experience;
         }), Codec.INT.fieldOf("cookingtime").orElse(pCookingTime).forGetter((p_296919_) -> {
            return p_296919_.cookingTime;
         })).apply(p_296927_, pFactory::create);
      });
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public T fromNetwork(FriendlyByteBuf pBuffer) {
      String s = pBuffer.readUtf();
      CookingBookCategory cookingbookcategory = pBuffer.readEnum(CookingBookCategory.class);
      Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
      ItemStack itemstack = pBuffer.readItem();
      float f = pBuffer.readFloat();
      int i = pBuffer.readVarInt();
      return this.factory.create(s, cookingbookcategory, ingredient, itemstack, f, i);
   }

   public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
      pBuffer.writeUtf(pRecipe.group);
      pBuffer.writeEnum(pRecipe.category());
      pRecipe.ingredient.toNetwork(pBuffer);
      pBuffer.writeItem(pRecipe.result);
      pBuffer.writeFloat(pRecipe.experience);
      pBuffer.writeVarInt(pRecipe.cookingTime);
   }

   public AbstractCookingRecipe create(String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime) {
      return this.factory.create(pGroup, pCategory, pIngredient, pResult, pExperience, pCookingTime);
   }
}