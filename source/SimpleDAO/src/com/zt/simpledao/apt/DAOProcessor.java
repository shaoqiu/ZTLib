package com.zt.simpledao.apt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.zt.simpledao.Column;
import com.zt.simpledao.Database;
import com.zt.simpledao.SQLDataType;
import com.zt.simpledao.Table;

@SupportedAnnotationTypes(value = { "*" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DAOProcessor extends AbstractProcessor {
	private Filer filer;
	private ColumnItem primary;
	private Map<Integer, ColumnItem> indexItemMap;

	private static class ColumnItem {
		public int index;
		public String name;
		public SQLDataType type;
	}

	@Override
	public void init(ProcessingEnvironment env) {
		filer = env.getFiler();
		indexItemMap = new HashMap<Integer, ColumnItem>();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getRootElements()) {
			String packageName = element.toString().substring(0,
					element.toString().lastIndexOf("."));
			String autoAPTPackageName = packageName + ".auto";
			String proxyClassName = element.getSimpleName() + "Proxy";

			if (element.toString().contains(autoAPTPackageName)) {
				// 不要循环为已经生成的类生成新的类
				continue;
			}

			createBeanProxy(autoAPTPackageName, proxyClassName, element);

			String daoClassName = element.getSimpleName() + "DAO";
			createDAO(autoAPTPackageName, daoClassName, proxyClassName, element);
		}
		return true;
	}

	private void createBeanProxy(String autoAPTPackageName, String proxyClassName,
			Element element) {
		StringBuilder proxyContent = new StringBuilder();
		proxyContent.append("package ").append(autoAPTPackageName).append(";\n");
		proxyContent.append("\nimport ").append(element.toString()).append(";\n");
		proxyContent.append("\nimport com.zt.simpledao.bean.IBeanProxy;\n");
		proxyContent.append("\npublic class ").append(proxyClassName)
				.append(" implements IBeanProxy {\n");
		proxyContent.append("	// ").append(element.toString()).append("\n");

		for (Element element2 : element.getEnclosedElements()) {
			if (element2.getKind().isField()) {
				Column c = element2.getAnnotation(Column.class);
				ColumnItem column = new ColumnItem();
				column.index = ((Column) c).index();
				column.name = ((Column) c).name();
				column.type = ((Column) c).type();
				if (0 == column.index) {
					// primary id
					primary = column;
				}
				indexItemMap.put(column.index, column);
				proxyContent.append("	public static final String ")
						.append(element2.getSimpleName()).append(" = ").append("\"")
						.append(c.name()).append("\";\n");
			}
		}

		Database db = element.getAnnotation(Database.class);
		proxyContent.append("	private static final String DATABASE_NAME = ")
				.append("\"").append(db.name()).append("\"").append(";\n");
		proxyContent.append("	private static final int VERSION = ")
				.append(db.version()).append(";\n");

		Table t = element.getAnnotation(Table.class);
		proxyContent.append("	private static final String TABLE = ").append("\"")
				.append(t.name()).append("\"").append(";\n");

		proxyContent.append("	private static final String TABLE_CREATOR = ")
				.append("\"").append(crateTable(t.name())).append("\"")
				.append(";\n");

		appendIPropMethod(proxyContent, element.getSimpleName().toString());

		proxyContent.append("\n}");

		JavaFileObject file = null;
		try {
			file = filer.createSourceFile(autoAPTPackageName + "/" + proxyClassName,
					element);
			file.openWriter().append(proxyContent).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String crateTable(String table) {
		StringBuilder sb = new StringBuilder();
		sb.append("create table ").append(table).append("(");
		int startIndex = 0;
		if (null == primary) {
			// 数据类未声明index=0的键，由我们创建一个。
			primary = new ColumnItem();
			primary.index = 0;
			primary.name = "_id";
			sb.append(primary.name).append(" integer primary key autoincrement, ");
			startIndex = 1;
		}
		final int total = indexItemMap.size() + startIndex;
		// 转换为了按Column中声明的index顺序构造sql语句。
		for (int i = startIndex; i < total; i++) {
			ColumnItem item = indexItemMap.get(i);
			sb.append(item.name).append(" ").append(item.type.toString());
			if (item.index == (total - 1)) {
				sb.append(");");
			} else {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	private void appendIPropMethod(StringBuilder sb, String className) {
		sb.append("\n	@Override\n");
		sb.append("	public String getDataBaseName() {\n").append(
				"		return DATABASE_NAME;\n	}\n");

		sb.append("\n	@Override\n");
		sb.append("	public int getDataBaseVersion() {\n").append(
				"		return VERSION;\n	}\n");

		sb.append("\n	@Override\n");
		sb.append("	public String getTableName() {\n").append(
				"		return TABLE;\n	}\n");

		sb.append("\n	@Override\n");
		sb.append("	public String getTableCreator() {\n").append(
				"		return TABLE_CREATOR;\n	}\n");

		sb.append("\n	@Override\n");
		sb.append("	public Class<?> getBeanClass() {\n").append("		return ")
				.append(className).append(".class;\n	}\n");
	}

	private void createDAO(String autoAPTPackageName, String daoClassName,
			String proxyClassName, Element element) {
		StringBuilder daoContent = new StringBuilder();
		// package
		daoContent.append("package ").append(autoAPTPackageName).append(";\n");
		// import
		daoContent.append("\nimport android.content.Context;\n");
		daoContent.append("import android.database.sqlite.SQLiteDatabase;\n");
		daoContent.append("import com.zt.simpledao.bean.IBeanProxy;\n");
		daoContent.append("import com.zt.simpledao.dao.sqlite.SQLite3DAO;\n");
		daoContent.append("import ").append(element.toString()).append(";\n");
		daoContent.append("import ").append(autoAPTPackageName).append(".")
				.append(proxyClassName).append(";\n");
		// class
		daoContent.append("\npublic class ").append(daoClassName)
				.append(" extends SQLite3DAO<").append(element.getSimpleName())
				.append("> {\n");
		daoContent.append("	// ").append(element.toString()).append("\n");
		// field
		daoContent.append("	private static ").append(daoClassName)
				.append(" sInstance;\n");
		// getInstance
		daoContent.append("\n	public synchronized static ").append(daoClassName)
				.append(" getInstance(Context context) {\n");
		daoContent.append("		if (null == sInstance) {\n");
		daoContent.append("			sInstance = new ").append(daoClassName)
				.append("(context, new ").append(proxyClassName)
				.append("());\n		}\n");
		daoContent.append("		return sInstance;\n	}\n");
		// constructor
		daoContent.append("\n	private ").append(daoClassName)
				.append("(Context context, IBeanProxy proxy) {\n");
		daoContent.append("		super(context, proxy);\n	}\n");
		// override
		daoContent.append("\n	@Override");
		daoContent
				.append("\n	protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, IBeanProxy proxy) {\n");
		daoContent
				.append("		db.execSQL(\"DROP TABLE IF EXISTS \" + proxy.getTableName());\n");
		daoContent.append("		db.execSQL(proxy.getTableCreator());\n	}\n");
		// end
		daoContent.append("\n}");
		// output
		JavaFileObject file = null;
		try {
			file = filer.createSourceFile(autoAPTPackageName + "/" + daoClassName,
					element);
			file.openWriter().append(daoContent).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
