package com.commsen.jpms.viz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Main {
 
	public static void main(String[] args) throws ClassNotFoundException, IOException, TemplateException {

		Render modulesView = new Render();
		Render servicesView = new Render();
		Render packagesView = new Render();
		Render allView = new Render();
		
		int baseModules = 0, modules = 0, serviceDefs = 0, services = 0;
		
		for (Module module : ModuleLayer.boot().modules()) {
			modules++;
			System.out.println("Processing " + module.getName());
			Node node = new Node();
			node.id = "m." + module.getName();
			node.name = module.getName();
			node.type = "Module";
			Set<Exports> exportedPackages = module.getDescriptor().exports();
			for (Exports exports : exportedPackages) {
				if (exports.source().startsWith("java.")) {
					baseModules++;
					node.type = "Base module";
					break;
				}
			}
			modulesView.nodes.add(node);
			servicesView.nodes.add(node);
			packagesView.nodes.add(node);
			allView.nodes.add(node);

			for (Exports exports : module.getDescriptor().exports()) {
				node = new Node();
				node.id = "p." + exports.source();
				node.name = exports.source();
				node.type = "Package";
				packagesView.nodes.add(node);
				allView.nodes.add(node);

				Link link = new Link();
				link.from = "m." + module.getName();
				link.to = "p." + exports.source();
				if (exports.isQualified()) {
					link.label = "Exports qualified";
				} else {
					link.label = "Exports";
				}
				packagesView.links.add(link);
				allView.links.add(link);

				for (String target: exports.targets()) {	
					link = new Link();
					link.from = "p." + exports.source();
					link.to = "m." + target;
					link.label = "Exported for";
					packagesView.links.add(link);
					allView.links.add(link);
				}
			}

			for (Requires requires : module.getDescriptor().requires()) {
				Link link = new Link();
				link.from = "m." + module.getName();
				link.to = "m." + requires.name();
				link.label = "Requires";
				modulesView.links.add(link);
				allView.links.add(link);
			}

			for (String service : module.getDescriptor().uses()) {
				serviceDefs++;
				node = new Node();
				node.id = "sd." + service;
				node.name = service;
				node.type = "Service definition";
				servicesView.nodes.add(node);
				allView.nodes.add(node);

				Link link = new Link();
				link.from = "m." + module.getName();
				link.to = "sd." + service;
				link.label = "Uses";
				servicesView.links.add(link);
				allView.links.add(link);
			}
			
			for (Provides provides : module.getDescriptor().provides()) {
				for (String provider : provides.providers()) {
					services++;
					node = new Node();
					node.id = "s." + provider;
					node.name = provider;
					node.type = "Service";
					servicesView.nodes.add(node);
					allView.nodes.add(node);

					Link link = new Link();
					link.from = "s." + provider;
					link.to = "sd." + provides.service();
					link.label = "Implements";
					servicesView.links.add(link);
					allView.links.add(link);

					link = new Link();
					link.from = "m." + module.getName();
					link.to = "s." + provider;
					link.label = "Provides";
					servicesView.links.add(link);
					allView.links.add(link);

				}
			}
		}
		
		System.out.println("All modules: " + modules);
		System.out.println("Base modules: " + baseModules);
		System.out.println("Service definitions: " + serviceDefs);
		System.out.println("Services: " + services);
		
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
		BeansWrapperBuilder wrapperBuilder = new BeansWrapperBuilder(Configuration.VERSION_2_3_27);
		wrapperBuilder.setExposeFields(true);
		
		cfg.setObjectWrapper(wrapperBuilder.build());
		cfg.setDirectoryForTemplateLoading(new File("."));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		
		
		Template temp = cfg.getTemplate("nodes.json.ftl");

		Map<String, Object> root = new HashMap<>();
		root.put("nodes", servicesView.nodes);
		root.put("links", servicesView.links);
		temp.process(root, new FileWriter(new File("html/services.json")));

		root.put("nodes", modulesView.nodes);
		root.put("links", modulesView.links);
		temp.process(root, new FileWriter(new File("html/modules.json")));

		root.put("nodes", packagesView.nodes);
		root.put("links", packagesView.links);
		temp.process(root, new FileWriter(new File("html/packages.json")));

		root.put("nodes", allView.nodes);
		root.put("links", allView.links);
		temp.process(root, new FileWriter(new File("html/all.json")));
	}

}		
	