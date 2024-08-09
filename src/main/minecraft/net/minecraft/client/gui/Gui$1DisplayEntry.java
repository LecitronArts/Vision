package net.minecraft.client.gui;

import net.minecraft.network.chat.Component;

record Gui$1DisplayEntry(Component name, Component score, int scoreWidth) {
   public Component name() {
      return this.name;
   }

   public Component score() {
      return this.score;
   }

   public int scoreWidth() {
      return this.scoreWidth;
   }
}
