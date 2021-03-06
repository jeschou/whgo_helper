package cn.whale.helper.repo;

import cn.whale.helper.template.SimpleTemplateRender;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.ui.TableRowData;
import cn.whale.helper.utils.DbConfig;
import cn.whale.helper.utils.GoUtils;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GoRepoGenerator {

    protected static Notifier notifier = new Notifier("whgo_helper gorm-repo-gen");

    private Project project;
    private VirtualFile selectedFile;

    private String fileName;
    private String database;
    private String tableName;
    private String structName;
    private List<TableRowData> fields;

    private DbConfig dbConfig;

    public GoRepoGenerator(Project project, VirtualFile selectedFile, DbConfig dbConfig, String fileName, String database, String tableName, String structName, List<TableRowData> fields) {
        this.project = project;
        this.selectedFile = selectedFile;
        this.fileName = fileName;
        this.database = database;
        this.tableName = tableName;
        this.structName = structName;
        this.fields = fields;
        if (!this.fileName.endsWith(".go")) {
            this.fileName = fileName + ".go";
        }
        this.dbConfig = dbConfig;
    }

    public void generate() {
        VirtualFile targetDir = selectedFile;
        if (!targetDir.isDirectory()) {
            targetDir = targetDir.getParent();
        }

        SimpleTemplateRender templateRender = new SimpleTemplateRender();
        try {
            templateRender.loadTemplate(GoRepoGenerator.class.getResourceAsStream("repo_template.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
            return;
        }

        Map<String, String> params = new HashMap<>();
        String packageName = targetDir.getName();
        List<VirtualFile> fs = IDEUtils.collectChild(targetDir, IDEUtils::isGoFile);
        if (!fs.isEmpty()) {
            try {
                String packageName0 = GoUtils.getPackage(IDEUtils.toFile(fs.get(0)), true);
                if (packageName0 != null) {
                    packageName = packageName0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        params.put("package", packageName);
        params.put("structName", Utils.unTitle(structName));
        params.put("StructName", Utils.toTitle(structName));
        params.put("tableName", tableName);
        params.put("database", database);
        params.put("Database", Utils.toTitleCamelCase(database));
        if ("common".equals(dbConfig.serviceName)) {
            params.put("serviceName", "");
        }
        if (dbConfig.database.equals(database)) {
            params.put("database", "");
        }

        List<String> imps = collectImport();


        int maxFieldWidth = 0;
        int maxTypeWidth = 0;
        for (TableRowData trd : fields) {
            maxFieldWidth = Math.max(maxFieldWidth, trd.fieldName.length());
            maxTypeWidth = Math.max(maxTypeWidth, trd.goType.length());
        }

        StringBuilder fieldsSb = new StringBuilder();
        for (TableRowData trd : fields) {
            fieldsSb.append("\t").append(String.format("%-" + maxFieldWidth + "s %-" + maxTypeWidth + "s %s", trd.fieldName, trd.goType, trd.tag)).append("\n");
        }
        if (fieldsSb.length() > 0) {
            fieldsSb.deleteCharAt(fieldsSb.length() - 1);
        }
        params.put("structFields", fieldsSb.toString());

        StringBuilder repoFieldsSb = new StringBuilder();
        for (TableRowData trd : fields) {
            repoFieldsSb.append("\t").append(String.format("%-" + maxFieldWidth + "s repo.FieldInterface", trd.fieldName)).append("\n");
        }
        if (repoFieldsSb.length() > 0) {
            repoFieldsSb.deleteCharAt(repoFieldsSb.length() - 1);
        }
        params.put("repoFieldStructFields", repoFieldsSb.toString());

        for (TableRowData trd : fields) {
            if ("is_delete".equals(trd.name) || "is_deleted".equals(trd.name)) {
                params.put("IsDelete", trd.fieldName);
            } else if ("update_time".equals(trd.name) || "updated_time".equals(trd.name)) {
                params.put("UpdateTime", trd.fieldName);
            }
        }

        StringBuilder impsSb = new StringBuilder();
        impsSb.append("\n");
        for (String s : imps) {
            impsSb.append("\t").append(Utils.quote(s)).append("\n");
        }
        params.put("imports", impsSb.toString());

        templateRender.render(params);

        File f = new File(targetDir.getPath(), fileName);
        try {
            Utils.writeFile(f, templateRender.getResult());
            RefreshQueue.getInstance().refresh(true, true, null, targetDir);
        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }

    }

    private List<String> collectImport() {
        Set<String> sets = new HashSet<>();
        sets.add("sync");
        sets.add("whgo/product/library/repo");
        for (TableRowData trd : fields) {
            String goType = trd.goType;
            if (goType.contains("time.")) {
                sets.add("time");
            }
            if (goType.contains("pq.")) {
                sets.add("github.com/lib/pq");
            }
            if (goType.contains("postgres.")) {
                sets.add("github.com/jinzhu/gorm/dialects/postgres");
            }
            if ("is_delete".equals(trd.name)||"is_deleted".equals(trd.name)) {
                sets.add("context");
                sets.add("whgo/product/library/utils");
            }
        }
        ArrayList<String> imps = new ArrayList<>(sets);
        GoUtils.sortGoImport(imps);
        return imps;
    }
}
