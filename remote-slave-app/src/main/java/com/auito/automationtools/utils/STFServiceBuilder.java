package com.auito.automationtools.utils;

import static com.auito.automationtools.utils.CommandLineExecutor.exec;
import static com.auito.automationtools.utils.CommandLineExecutor.killProcess;
import static com.auito.automationtools.utils.StringUtils.getLocalNetworkIP;
import static com.auito.automationtools.utils.StringUtils.getProperty;
import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

public final class STFServiceBuilder {

	private static final String DEFAULT_STF_SCREENSHOT_QUALITY = "10";

	private STFServiceBuilder() {
	}

	public static Builder builder() {
		return new Builder();
	}

	@Slf4j
	public static final class Builder {

		public boolean isSTFRunning() {
			CommandLineResponse response = exec("ps -ef | grep -i 'stf' | grep -v grep | wc -l");
			boolean isRunning = Integer.parseInt(response.getStdOut().trim()) > 0;
			if (isRunning) {
				log.debug("STF service is running");
			} else {
				log.error("STF service is not running");
			}
			return isRunning;
		}

		public void restart() {
			stop();
			start();
		}

		public void stop() {
			log.info("stopping STF service");
			killProcess("start_stf.bash", true);
		}

		public void start() {
			String script = StringUtils
					.streamToString(STFServiceBuilder.class.getResourceAsStream("/scripts/stf.bash"));
			File scriptFile = new File("start_stf.bash");
			log.info("starting STF service");
			new Thread(() -> {
				try {
					java.nio.file.Files.write(scriptFile.toPath(), script.getBytes());
					String cmd = String.format("bash %s %s %s", scriptFile.getAbsolutePath(), getLocalNetworkIP(),
							getProperty("STF_SCREENSHOT_QUALITY", DEFAULT_STF_SCREENSHOT_QUALITY));
					getRuntime().exec(cmd).waitFor(5, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					scriptFile.delete();
				}
			}).start();
		}

	}
}
