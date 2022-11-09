package com.dependency.manager.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Controller
public class ApiController {

    @ResponseBody
    @RequestMapping("/api/alive")
    public String test() {
        return "I'm alive!";
    }

    /**
     * http://127.0.0.1:8080/api/dependency_list
     */
    @ResponseBody
    @RequestMapping("/api/dependency/list")
    public String dependencyApi() {
        return JSONObject.toJSONString(loadDependencyUpdateList());
    }


    @ResponseBody
    @RequestMapping("/api/analysis/dependency")
    public String checkUpdate(@RequestParam(name = "gitUrl") String gitUrl,
                              @RequestParam(name = "branch") String branch) {
        log.info("param gitUrl={}", gitUrl);
        log.info("param branch={}", branch);

        JSONObject resp = new JSONObject();
        resp.put("status", 0);
        Map<String, DependencyVersion> configMap = loadDependencyUpdateJsonConfig();
        if (configMap.size() == 0) {
            resp.put("status", 1);
            resp.put("msg", "系统设置的配置升级项列表为空，不需要升级，退出！");
            return resp.toJSONString();
        }
        try {
            String projectName = gitUrl.substring(gitUrl.lastIndexOf("/") + 1, gitUrl.lastIndexOf(".git"));
            Runtime rt = Runtime.getRuntime();
            String[] commands = {ResourceUtils.getURL("").getPath() + "dependency.sh", gitUrl, branch, projectName};
            Process proc = rt.exec(commands);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            // Read the output from the command
            //System.out.println("Here is the standard output of the command:\n");
            String s = null;
            Map<String, DependencyVersion> map = new HashMap<>();
            List<DependencyVersion> list = new ArrayList<DependencyVersion>();
            boolean start = false;
            boolean end = false;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if (s.contains("Dependencies for source")) {
                    start = true;
                    continue;
                }
                if (start && s.contains("No dependencies")) {
                    end = true;
                    break;
                }
                if (!start) {
                    continue;
                }
                if (!s.contains("---") && !s.contains(":")) {
                    continue;
                }
                String[] sa = s.split("---");
                if (sa.length < 2) {
                    continue;
                }
                //System.out.println("---->" + s);
                String version = sa[1].trim();
                //处理如下格式的依赖：
                //1）com.google.guava:guava:19.0 -> 23.0
                //2）com.google.guava:guava:19.0 -> 23.0 (*)
                //3）com.fasterxml.jackson.core:jackson-databind:2.8.10 (*)
                //4）com.didapinche:akso-java:1.0.0
                if (version.contains("->")) {
                    String[] arr = version.split("->");
                    String[] sarr = arr[0].split(":");
                    DependencyVersion vr = new DependencyVersion();
                    vr.setGroupId(sarr[0]);
                    vr.setArtifactId(sarr[1]);
                    String newVersion = arr[1];
                    if (newVersion.contains("(")) {
                        vr.setVersion(newVersion.split("\\(")[0].trim());
                    } else {
                        vr.setVersion(newVersion.trim());
                    }
                    String key = vr.getGroupId() + ":" + vr.getArtifactId();
                    map.put(key, vr);
                    list.add(vr);
                } else {
                    if (version.contains("(")) {
                        version = version.split("\\(")[0];
                    }
                    String[] sarr = version.split(":");
                    DependencyVersion vr = new DependencyVersion();
                    vr.setGroupId(sarr[0]);
                    vr.setArtifactId(sarr[1]);
                    vr.setVersion(sarr[2].trim());

                    String key = vr.getGroupId() + ":" + vr.getArtifactId();
                    map.put(key, vr);
                    list.add(vr);
                }
            }
            if (map.size() == 0) {
                resp.put("status", 2);
                resp.put("msg", "当前项目未获取有效的版本依赖关系，请检测项目配置！");
                return resp.toJSONString();
            }
            //需要新增的
            List<DependencyVersion> newList = new ArrayList<>();
            //需要更新的
            List<DependencyVersion> updateList = new ArrayList<>();
            //需要排除的
            List<DependencyVersion> excludeList = new ArrayList<>();

            //处理 版本升级&移除
            for (Map.Entry<String, DependencyVersion> ent : map.entrySet()) {
                String key = ent.getKey();
                DependencyVersion item = ent.getValue();
                //先处理 要排除 的依赖，支持 正则表达式
                for (Map.Entry<String, DependencyVersion> e : configMap.entrySet()) {
                    String regex = e.getKey();
                    DependencyVersion value = e.getValue();
                    if (Objects.equals("exclude", value.getTyp())) {
                        //System.out.println("regex.matches(key) ->" + regex + " \t " + key);
                        if (Objects.equals(key, regex) || isMatch(key, regex)) {
                            excludeList.add(item);
                            break;
                        }
                    }
                }
                if (!configMap.containsKey(key)) {
                    continue;
                }
                /* typ 类型：new 新增；update 更新； exclude 排除/移除
                 * opt 操作：force 强制升级；option 可选
                 */
                DependencyVersion configItem = configMap.get(key);
                if (Objects.equals(configItem.getTyp(), "new") || Objects.equals(configItem.getTyp(), "update") && !item.getVersion().equals(configItem.getVersion())) {
                    updateList.add(configItem);
                }
            }
            //处理新增
            for (Map.Entry<String, DependencyVersion> e : configMap.entrySet()) {
                String key = e.getKey();
                DependencyVersion item = e.getValue();
                if (Objects.equals(item.getTyp(), "new") || Objects.equals(item.getTyp(), "update") && !map.containsKey(key) && !Objects.equals(item.getOpt(), "option")) {
                    newList.add(item);
                }
            }
            resp.put("newList", newList);
            resp.put("updateList", updateList);
            resp.put("excludeList", excludeList);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return resp.toJSONString();
    }

    private Map<String, DependencyVersion> loadDependencyUpdateJsonConfig() {
        List<DependencyVersion> list = loadDependencyUpdateList();
        if (Objects.isNull(list) || list.size() == 0) {
            return new HashMap<>();
        }
        Map<String, DependencyVersion> map = new HashMap<>();
        for (DependencyVersion item : list) {
            map.put(item.getGroupId() + ":" + item.getArtifactId(), item);
        }
        return map;
    }

    private List<DependencyVersion> loadDependencyUpdateList() {
        String path = "dependency-update.json";
        try {
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(ApiController.class.getClassLoader().getResourceAsStream(path)));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line + "");
                line = br.readLine();
            }
            return JSONArray.parseArray(sb.toString(), DependencyVersion.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static boolean isMatch(String s, String p) {
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int i = 1; i <= n; ++i) {
            if (p.charAt(i - 1) == '*') {
                dp[0][i] = true;
            } else {
                break;
            }
        }
        for (int i = 1; i <= m; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (p.charAt(j - 1) == '*') {
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (p.charAt(j - 1) == '?' || s.charAt(i - 1) == p.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }

    public static class DependencyVersion {
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
        /**
         * typ 类型：new 新增；update 更新； exclude 排除/移除
         */
        private String typ;
        /**
         * opt 操作：force 强制升级；option 可选
         */
        private String opt;
        /**
         * 说明
         */
        private String remark;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getTyp() {
            return typ;
        }

        public void setTyp(String typ) {
            this.typ = typ;
        }

        public String getOpt() {
            return opt;
        }

        public void setOpt(String opt) {
            this.opt = opt;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }


}
