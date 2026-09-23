package com.awesomecopilot.common.lang.resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * P2-7 / P2-8 / P2-20 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P2-7 getInt(property, defaultValue) 复用 getInt(property) 再拿 -1 当"没有值"的哨兵：
 * 修复前实测配置文件里真的写了 -1 时，默认值把真实值顶掉（getInt("probe.neg",5) 返回 5）；</li>
 * <li>P2-8 两个 FileInputStream 交给 PropertyResourceBundle 后没人关：
 * 修复前实测（Windows）构造完成后立刻删文件失败（句柄被文件锁住），
 * System.gc() 触发 Cleaner 后才删得掉——句柄要等到 GC 才释放；</li>
 * <li>P2-20 YamlReader.loadFirst 解析失败时流不关闭、YAMLException 直接穿出构造器：
 * 修复前一个语法错误的 config 覆盖文件会让整个 YamlReader 构造失败，
 * classpath 里的默认配置回退不了。修复后跳过坏文件继续找下一优先级。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ResourceP2FixTest {

	private static final String WORKING_DIR = System.getProperty("user.dir");

	private final java.util.List<File> created = new java.util.ArrayList<>();

	private File touch(String relPath, String content) throws Exception {
		File f = new File(WORKING_DIR, relPath);
		f.getParentFile().mkdirs();
		try (Writer w = new FileWriter(f)) {
			w.write(content);
		}
		created.add(f);
		return f;
	}

	@AfterEach
	public void cleanup() throws Exception {
		for (File f : created) {
			Files.deleteIfExists(f.toPath());
		}
		//清掉 ResourceBundle/Yaml 可能留下的缓存目录项不需要额外处理: 文件名带进程 pid 后缀
	}

	private static String name(String base) {
		return base + "-" + ProcessHandle.current().pid();
	}

	// ---------------- P2-7 -1 哨兵顶掉真实值 ----------------

	@Test
	public void testGetIntWithDefaultKeepsRealMinusOneValue() throws Exception {
		String res = name("p2probe-int");
		touch(res + ".properties", "probe.neg=-1\nprobe.pos=7\nprobe.txt=abc\n");

		PropertyReader reader = new PropertyReader(res);
		//修复前实测: 配置里真的写了 -1, getInt("probe.neg", 5) 返回 5(真实值被默认值顶掉)
		assertThat(reader.getInt("probe.neg", 5))
				.as("配置里的真实值 -1 不能被默认值顶掉(修复前实测返回 5)")
				.isEqualTo(-1);
		assertThat(reader.getInt("probe.pos", 5)).isEqualTo(7);          //正常值不受影响
		assertThat(reader.getInt("probe.missing", 5)).isEqualTo(5);      //真缺失走默认值
		assertThat(reader.getInt("probe.txt", 5)).isEqualTo(5);          //解析失败走默认值
	}

	// ---------------- P2-8 文件句柄构造完即释放 ----------------

	@Test
	public void testWorkingDirPropertiesFileHandleReleasedAfterConstruction() throws Exception {
		String res = name("p2probe-handle");
		File f = touch(res + ".properties", "handle.test=1\n");

		new PropertyReader(res);

		//修复前实测(Windows): 构造完立刻删失败(句柄被 PropertyResourceBundle 里的
		//FileInputStream 锁住), System.gc() 触发 Cleaner 后才删得掉——句柄拖到 GC 才释放。
		//修复后构造器内部 try-with-resources, 出构造即关, 立刻可删。
		assertThat(f.canWrite()).isTrue();
		boolean deleted = f.delete();
		assertThat(deleted)
				.as("修复前实测: 构造完成后立刻 delete() 返回 false(句柄未释放), 修复后必为 true")
				.isTrue();
		created.remove(f); //已删掉, 不必再清理
	}

	@Test
	public void testConfigDirPropertiesFileHandleReleasedAfterConstruction() throws Exception {
		String res = name("p2probe-cfg");
		File f = touch("config" + File.separator + res + ".properties", "cfg.test=1\n");

		new PropertyReader(res);

		boolean deleted = f.delete();
		assertThat(deleted)
				.as("config 目录那条 FileInputStream 同样要构造完即关(修复前实测删不掉)")
				.isTrue();
		created.remove(f);
	}

	// ---------------- P2-20 坏 YAML 不让构造失败, 回退低优先级 ----------------

	@Test
	public void testBrokenOverrideYamlFallsBackToClasspathDefaults() throws Exception {
		String res = name("p2broadyml");
		//classpath 里放一份合法的
		Path classesDir = new File(WORKING_DIR, "target/test-classes").toPath();
		File cpFile = classesDir.resolve(res + ".yml").toFile();
		cpFile.getParentFile().mkdirs();
		try (Writer w = new FileWriter(cpFile)) {
			w.write("app:\n  name: good-default\n");
		}
		created.add(cpFile);
		//工作目录 config 下放一份语法错误的(高优先级)
		File broken = touch("config" + File.separator + res + ".yml", "app: {name: unclosed\n  bad: [unclosed\n");

		//修复前实测: YAMLException(运行时异常)直接穿出, 整个构造失败, classpath 默认值回退不了
		final YamlReader[] holder = new YamlReader[1];
		assertThatCode(() -> holder[0] = new YamlReader(res))
				.as("高优先级文件语法错误时应跳过它, 回退到 classpath 的正常配置")
				.doesNotThrowAnyException();
		assertThat(holder[0].getString("app.name")).isEqualTo("good-default");

		//解析失败路径同样要关闭输入流(修复前流没关, Windows 上文件被锁住删不掉)
		assertThat(broken.delete())
				.as("坏 YAML 被跳过后, 它的输入流必须已关闭(修复前锁着删不掉)")
				.isTrue();
		created.remove(broken);
	}
}
