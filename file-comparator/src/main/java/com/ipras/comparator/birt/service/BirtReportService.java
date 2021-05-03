package com.ipras.comparator.birt.service;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.datatools.connectivity.oda.design.util.DesignAdapterFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class BirtReportService implements ApplicationContextAware, DisposableBean {

	@Autowired
	private ServletContext servletContext;

	private IReportEngine birtEngine;
	private ApplicationContext context;

	@SuppressWarnings("unchecked")
	@PostConstruct
	protected void initialize() throws BirtException {
		EngineConfig config = new EngineConfig();
		config.getAppContext().put("spring", this.context);
		Platform.startup(config);
		IReportEngineFactory factory = (IReportEngineFactory) Platform
				.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
		birtEngine = factory.createReportEngine(config);
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.context = context;
	}

	public void generateMainReport(ReportDesignHandle design, HttpServletResponse response,
			HttpServletRequest request) throws EngineException {

		generateHTMLReport(design, response, request);

	}

	/**
	 * Generate a report as HTML
	 * @throws EngineException 
	 */
	@SuppressWarnings("unchecked")
	private void generateHTMLReport(ReportDesignHandle design, HttpServletResponse response, HttpServletRequest request) throws EngineException {
		IReportRunnable runnableDesign = birtEngine.openReportDesign(design);
		IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(runnableDesign);
		response.setContentType(birtEngine.getMIMEType("html"));
		IRenderOption options = new RenderOption();
		HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
		htmlOptions.setOutputFormat("html");
		runAndRenderTask.setRenderOption(htmlOptions);
		runAndRenderTask.getAppContext().put(EngineConstants.APPCONTEXT_BIRT_VIEWER_HTTPSERVET_REQUEST, request);

		try {
			htmlOptions.setOutputStream(response.getOutputStream());
			runAndRenderTask.run();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			runAndRenderTask.close();
			design.close();
		}
	}

	@Override
	public void destroy() {
		
		birtEngine.destroy();
		Platform.shutdown();
	}

}
