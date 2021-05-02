package indi.goldenwater.healthdisplay.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class I18nManager {
    private final Map<String, Map<String, String>> languages = new HashMap<>();
    private String defaultLanguage = "en_us";
    private boolean initialized = false;
    private File dataPath;

    private final Gson gson = new Gson();

    public I18nManager(File dataPath, String langFolder, String defaultLanguage) {
        this(new File(dataPath.getPath(), langFolder), defaultLanguage);
    }

    public I18nManager(File path) {
        this(path, "en_us");
    }

    public I18nManager(File path, String defaultLanguage) {
        if (initialize(path, defaultLanguage.toLowerCase()) == StatusCode.success) initialized = true;
    }

    public int initialize(File languageFilePath) {
        return initialize(languageFilePath, "en_us");
    }

    public int initialize(File dataPath, String langFolder, String defaultLanguage) {
        return initialize(new File(dataPath.getPath(), langFolder), defaultLanguage);
    }

    public int initialize(File languageFilePath, String defaultLanguage) {
        this.dataPath = languageFilePath;
        this.defaultLanguage = defaultLanguage;

        if (!languageFilePath.exists()) {
            if (!languageFilePath.mkdirs()) { // 如果目录不存在则创建目录
                return StatusCode.failToCreateFolder; // 如果创建失败则返回错误
            }
        }
        if (!languageFilePath.isDirectory()) return StatusCode.pathError; // 如果不是一个目录则返回错误

        File[] files = languageFilePath.listFiles(file -> file.getName().endsWith(".json"));// 获取目录下所有json文件

        if (files == null) return StatusCode.success;

        for (File file : files) {
            if (!file.canRead()) return StatusCode.failToRead;

            try {
                Path path = Paths.get(file.getPath());
                byte[] data = Files.readAllBytes(path);
                String jsonStr = new String(data, StandardCharsets.UTF_8); // 读取内容

                Map<String, String> lang = gson.fromJson(jsonStr, new TypeToken<Map<String, String>>() {
                }.getType());

                languages.put(file.getName().replaceAll("\\.json", "").toLowerCase(), lang);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return StatusCode.success;
    }

    public String getL10n(String lang, String key) {
        if (!initialized) return ""; // 检查 初始化 是否完成
        Map<String, String> langData = languages.get(lang.toLowerCase()); // 获取 指定语言的 数据

        if (langData == null || langData.get(key) == null) { // 如果 指定语言不存在 或 指定语言中 没有 指定数据
            Map<String, String> defaultLangData = languages.get(defaultLanguage); // 获取 默认语言的 数据

            if (defaultLangData == null || defaultLangData.get(key) == null) { // 如果 默认语言不存在 或 默认语言中 没有 指定数据
                return key; // 返回 键
            } else {
                return defaultLangData.get(key); // 返回 默认语言中 的 指定数据
            }
        }

        return langData.get(key); // 返回 指定语言中 的 指定数据
    }

    public void releaseDefaultLangFile(JavaPlugin plugin, String folderInJar,
                                       String langListFileName, boolean replace) {
        InputStream inputStream = plugin.getResource(folderInJar + "/" + langListFileName); // 获取语言列表文件

        if (inputStream == null) return; // 如果为空则退出

        try {
            int fileLength = inputStream.available(); // 获取文件长度
            byte[] data = new byte[fileLength]; // 分配数据缓存

            if (inputStream.read(data) != -1) { // 如果有数据
                String jsonStr;

                jsonStr = new String(data, StandardCharsets.UTF_8); // byte转字符串
                jsonStr = jsonStr.replaceAll("\n", "")
                        .replaceAll("\r", ""); // 去除 LF 和 CR

                Map<String, String[]> languages =
                        gson.fromJson(jsonStr, new TypeToken<Map<String, String[]>>() {
                        }.getType()); // 读取json至Map

                String[] fileNameArrays = dataPath.list((dir, name) -> name.endsWith(".json")); // 获取现有语言文件名数组
                List<String> fileNameList = new ArrayList<>(); // 初始化文件名 List
                if (fileNameArrays != null) fileNameList = Arrays.asList(fileNameArrays); //如果文件列表不为空 转至List

                for (String langCode : languages.get("languages")) { // 遍历所有语言
                    if (!fileNameList.contains(langCode + ".json") || replace) { // 如果 文件不存在 或 启用强制替换 则
                        plugin.saveResource(folderInJar + "/" + langCode + ".json",
                                replace); // 释放相应文件
                    }
                }

                this.reload(); // 重载
            }
        } catch (IOException ignored) {
        }
    }

    public List<String> getLanguageList() {
        List<String> languageList = new ArrayList<>();

        if (!initialized) return languageList; // 检查 初始化 是否完成

        languageList = Arrays.asList(languages.keySet().toArray(new String[]{}));
        return languageList;
    }

    public void setLanguage(JavaPlugin plugin,
                            CommandSender sender,
                            String lang,
                            String targetLanguage,
                            String configKey,
                            String langKeyList,
                            String langKeySuccessSet,
                            String langKeySetFail,
                            String langKeySetFailDoesNotExist) {
        if (targetLanguage == null) {
            List<String> languageList = this.getLanguageList();
            StringBuilder languageListStr = new StringBuilder();

            for (String str : languageList) { // 将语言列表转为字符串
                boolean isLast = str.equals(languageList.get(languageList.size() - 1));
                languageListStr.append(str).append(isLast ? "." : ", ");
            }

            String finalMessage = this.getL10n(lang, langKeyList)
                    .replace("{{languages}}", languageListStr.toString()); // 将语言列表放入消息

            sender.sendMessage(finalMessage);
        } else {
            String message;
            if (this.getLanguageList().contains(targetLanguage.toLowerCase())) {
                Configuration config = plugin.getConfig();

                config.set(configKey, targetLanguage);
                plugin.saveConfig();

                lang = config.getString(configKey);

                message = this.getL10n(lang, langKeySuccessSet);
            } else {
                message = this.getL10n(lang, langKeySetFail);
                message += "\n    ";
                message += this.getL10n(lang, langKeySetFailDoesNotExist);
            }

            message = message.replace("{{language}}", targetLanguage);

            sender.sendMessage(message);
        }
    }

    public void reload() {
        this.initialized = initialize(dataPath, defaultLanguage.toLowerCase()) == StatusCode.success;
    }

    public static class StatusCode {
        public static final int success = 0;
        public static final int pathError = 1;
        public static final int failToCreateFolder = 2;
        public static final int failToRead = 3;
    }
}
