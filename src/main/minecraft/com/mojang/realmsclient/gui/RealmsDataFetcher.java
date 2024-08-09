package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.gui.task.RepeatedDelayStrategy;
import com.mojang.realmsclient.util.RealmsPersistence;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsDataFetcher {
   public final DataFetcher dataFetcher = new DataFetcher(Util.ioPool(), TimeUnit.MILLISECONDS, Util.timeSource);
   private final List<DataFetcher.Task<?>> tasks;
   public final DataFetcher.Task<List<RealmsNotification>> notificationsTask;
   public final DataFetcher.Task<RealmsDataFetcher.ServerListData> serverListUpdateTask;
   public final DataFetcher.Task<Integer> pendingInvitesTask;
   public final DataFetcher.Task<Boolean> trialAvailabilityTask;
   public final DataFetcher.Task<RealmsNews> newsTask;
   public final RealmsNewsManager newsManager = new RealmsNewsManager(new RealmsPersistence());

   public RealmsDataFetcher(RealmsClient pRealmsClient) {
      this.serverListUpdateTask = this.dataFetcher.createTask("server list", () -> {
         com.mojang.realmsclient.dto.RealmsServerList realmsserverlist = pRealmsClient.listWorlds();
         return RealmsMainScreen.isSnapshot() ? new RealmsDataFetcher.ServerListData(realmsserverlist.servers, pRealmsClient.listSnapshotEligibleRealms()) : new RealmsDataFetcher.ServerListData(realmsserverlist.servers, List.of());
      }, Duration.ofSeconds(60L), RepeatedDelayStrategy.CONSTANT);
      this.pendingInvitesTask = this.dataFetcher.createTask("pending invite count", pRealmsClient::pendingInvitesCount, Duration.ofSeconds(10L), RepeatedDelayStrategy.exponentialBackoff(360));
      this.trialAvailabilityTask = this.dataFetcher.createTask("trial availablity", pRealmsClient::trialAvailable, Duration.ofSeconds(60L), RepeatedDelayStrategy.exponentialBackoff(60));
      this.newsTask = this.dataFetcher.createTask("unread news", pRealmsClient::getNews, Duration.ofMinutes(5L), RepeatedDelayStrategy.CONSTANT);
      this.notificationsTask = this.dataFetcher.createTask("notifications", pRealmsClient::getNotifications, Duration.ofMinutes(5L), RepeatedDelayStrategy.CONSTANT);
      this.tasks = List.of(this.notificationsTask, this.serverListUpdateTask, this.pendingInvitesTask, this.trialAvailabilityTask, this.newsTask);
   }

   public List<DataFetcher.Task<?>> getTasks() {
      return this.tasks;
   }

   @OnlyIn(Dist.CLIENT)
   public static record ServerListData(List<RealmsServer> serverList, List<RealmsServer> availableSnapshotServers) {
   }
}