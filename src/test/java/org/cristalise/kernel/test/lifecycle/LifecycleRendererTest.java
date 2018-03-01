package org.cristalise.kernel.test.lifecycle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.renderer.LifecycleRenderer;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class LifecycleRendererTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        Logger.addLogStream(System.out, 8);

        Properties props = FileStringUtility.loadConfigFile(LifecycleRendererTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    @Test
    public void generateDef_PNG() throws Exception {
        String caDefXML = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/CA/ManageModule.xml"));
        CompositeActivityDef caDef = (CompositeActivityDef) Gateway.getMarshaller().unmarshall(caDefXML);

        BufferedImage img = new LifecycleRenderer(caDef.getChildrenGraphModel(), true).getWorkFlowModelImage(500, 500);
        ImageIO.write(img, "png", new File("target/ManageModule.png"));
    }

    @Test
    public void generateDef_SVG() throws Exception {
        String caDefXML = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/CA/ManageModule.xml"));
        CompositeActivityDef caDef = (CompositeActivityDef) Gateway.getMarshaller().unmarshall(caDefXML);

        LifecycleRenderer generator = new LifecycleRenderer(caDef.getChildrenGraphModel(), true);
        int zoomFactor = generator.getZoomFactor(500, 500);

        SVGGraphics2D svgG2D =  new SVGGraphics2D(500, 500);
        svgG2D.scale((double) zoomFactor / 100, (double) zoomFactor / 100);

        generator.draw(svgG2D);

        SVGUtils.writeToSVG(new File("target/ManageModule.svg"), svgG2D.getSVGElement());
    }

    @Test
    public void generateInstance_SVG() throws Exception {
        String wfXML = FileStringUtility.url2String(LifecycleRendererTest.class.getResource("/Lifecycle.workflow"));

        Workflow wf = (Workflow) Gateway.getMarshaller().unmarshall(wfXML);

        LifecycleRenderer generator = new LifecycleRenderer(wf.search("workflow/domain").getChildrenGraphModel(), false);
        int zoomFactor = generator.getZoomFactor(500, 500);

        SVGGraphics2D svgG2D =  new SVGGraphics2D(500, 500);
        svgG2D.scale((double) zoomFactor / 100, (double) zoomFactor / 100);

        generator.draw(svgG2D);

        SVGUtils.writeToSVG(new File("target/workflow.svg"), svgG2D.getSVGElement());
    }
}
