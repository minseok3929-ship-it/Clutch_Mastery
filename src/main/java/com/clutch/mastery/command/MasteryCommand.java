package com.clutch.mastery.command;

import com.clutch.mastery.db.DatabaseManager;
import com.clutch.mastery.gui.MasteryGUI;
import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.model.MasteryData;
import com.clutch.mastery.model.MasteryRankEntry;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Stream;

public class MasteryCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final MasteryManager masteryManager;
    private final DatabaseManager databaseManager;
    private final MasteryGUI masteryGUI;

    public MasteryCommand(JavaPlugin plugin, MasteryManager masteryManager, DatabaseManager databaseManager, MasteryGUI masteryGUI) {
        this.plugin = plugin;
        this.masteryManager = masteryManager;
        this.databaseManager = databaseManager;
        this.masteryGUI = masteryGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MasteryManager.PREFIX + "플레이어만 사용할 수 있습니다.");
                return true;
            }
            masteryGUI.open(player);
            return true;
        }

        switch (args[0]) {
            case "정보" -> showInfo(sender);
            case "랭킹" -> showRanking(sender, args.length >= 2 ? args[1] : "전체");
            case "지급" -> giveExp(sender, args);
            case "설정" -> setLevel(sender, args);
            case "초기화" -> reset(sender, args);
            default -> sender.sendMessage(MasteryManager.PREFIX + "사용법: /숙련도 [정보|랭킹|지급|설정|초기화]");
        }
        return true;
    }

    private void showInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MasteryManager.PREFIX + "플레이어만 사용할 수 있습니다.");
            return;
        }
        sender.sendMessage(MasteryManager.PREFIX + "§a" + player.getName() + "님의 숙련도 정보");
        for (MasteryType type : MasteryType.values()) {
            MasteryData data = masteryManager.getData(player.getUniqueId(), type);
            sender.sendMessage("§7- §f" + type.getKoreanName() + ": §aLv." + data.getLevel() + " §7(" + data.getExp() + "/" + masteryManager.getRequiredExp(data.getLevel()) + ")");
        }
    }

    private void showRanking(CommandSender sender, String input) {
        MasteryType type = null;
        if (!"전체".equals(input)) {
            Optional<MasteryType> parsed = MasteryType.parse(input);
            if (parsed.isEmpty()) {
                sender.sendMessage(MasteryManager.PREFIX + "랭킹 종류는 전체, 농사, 채광, 낚시 중 하나입니다.");
                return;
            }
            type = parsed.get();
        }
        MasteryType rankingType = type;
        databaseManager.loadRanking(type).thenAccept(entries -> Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(MasteryManager.PREFIX + "§a숙련도 랭킹 TOP 10 §7(" + (rankingType == null ? "전체" : rankingType.getKoreanName()) + ")");
            if (entries.isEmpty()) {
                sender.sendMessage("§7랭킹 데이터가 없습니다.");
                return;
            }
            int rank = 1;
            for (MasteryRankEntry entry : entries) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid());
                String name = offlinePlayer.getName() == null ? entry.uuid().toString() : offlinePlayer.getName();
                sender.sendMessage("§e" + rank++ + ". §f" + name + " §7- 레벨 " + entry.levelScore() + ", EXP " + entry.expScore());
            }
        }));
    }

    private void giveExp(CommandSender sender, String[] args) {
        if (!requireOp(sender) || !requireArgs(sender, args, 4, "/숙련도 지급 <플레이어> <종류> <경험치>")) {
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        Optional<MasteryType> type = MasteryType.parse(args[2]);
        Integer amount = parseInt(args[3]);
        if (target == null || type.isEmpty() || amount == null || amount <= 0) {
            sender.sendMessage(MasteryManager.PREFIX + "플레이어, 종류 또는 경험치가 올바르지 않습니다.");
            return;
        }
        masteryManager.addExp(target, type.get(), amount);
        sender.sendMessage(MasteryManager.PREFIX + "경험치를 지급했습니다.");
    }

    private void setLevel(CommandSender sender, String[] args) {
        if (!requireOp(sender) || !requireArgs(sender, args, 4, "/숙련도 설정 <플레이어> <종류> <레벨>")) {
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        Optional<MasteryType> type = MasteryType.parse(args[2]);
        Integer level = parseInt(args[3]);
        if (target == null || type.isEmpty() || level == null) {
            sender.sendMessage(MasteryManager.PREFIX + "플레이어, 종류 또는 레벨이 올바르지 않습니다.");
            return;
        }
        masteryManager.setLevel(target.getUniqueId(), type.get(), level);
        masteryManager.savePlayer(target.getUniqueId());
        sender.sendMessage(MasteryManager.PREFIX + "숙련도 레벨을 설정했습니다.");
    }

    private void reset(CommandSender sender, String[] args) {
        if (!requireOp(sender) || !requireArgs(sender, args, 2, "/숙련도 초기화 <플레이어>")) {
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MasteryManager.PREFIX + "온라인 플레이어만 초기화할 수 있습니다.");
            return;
        }
        masteryManager.reset(target.getUniqueId());
        sender.sendMessage(MasteryManager.PREFIX + "숙련도 데이터를 초기화했습니다.");
    }

    private boolean requireOp(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(MasteryManager.PREFIX + "OP만 사용할 수 있습니다.");
            return false;
        }
        return true;
    }

    private boolean requireArgs(CommandSender sender, String[] args, int length, String usage) {
        if (args.length < length) {
            sender.sendMessage(MasteryManager.PREFIX + usage);
            return false;
        }
        return true;
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], sender.isOp() ? List.of("정보", "랭킹", "지급", "설정", "초기화") : List.of("정보", "랭킹"));
        }
        if (args.length == 2 && (args[0].equals("지급") || args[0].equals("설정") || args[0].equals("초기화"))) {
            return filter(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 2 && args[0].equals("랭킹")) {
            return filter(args[1], List.of("전체", "농사", "채광", "낚시"));
        }
        if (args.length == 3 && (args[0].equals("지급") || args[0].equals("설정"))) {
            return filter(args[2], Stream.of(MasteryType.values()).flatMap(type -> Stream.of(type.name(), type.getKoreanName())).toList());
        }
        return List.of();
    }

    private List<String> filter(String input, List<String> options) {
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))).toList();
    }
}
