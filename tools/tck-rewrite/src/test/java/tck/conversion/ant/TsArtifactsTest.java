package tck.conversion.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import com.sun.ts.tests.ejb.ee.bb.session.lrapitest.B;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.ant.AttributeMap;
import tck.jakarta.platform.ant.ClientJar;
import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.EjbJar;
import tck.jakarta.platform.ant.PackageTarget;
import tck.jakarta.platform.ant.ProjectWrapper;
import tck.jakarta.platform.ant.TsTaskListener;
import tck.jakarta.platform.ant.Utils;
import tck.jakarta.platform.ant.Vehicles;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * These tests only run if there is ts.home property set.
 */
@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class TsArtifactsTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    /**
     * Validate the expected locations in the EE 10 TCK dist pointed to by the ts.home system property
     */
    @Test
    public void validateTsHome() {
        String tsHomeProp = System.getProperty("ts.home");
        Assertions.assertNotNull(tsHomeProp);
        Path tsHome = Paths.get(tsHomeProp);
        Path bin = tsHome.resolve("bin");
        Path classes = tsHome.resolve("classes");
        Path vehicles = tsHome.resolve("classes/com/sun/ts/tests/common/vehicle");
        Path ejbliteServlet = tsHome.resolve("src/com/sun/ts/tests/common/vehicle/ejbliteservlet/EJBLiteServletVehicle.java.txt");
        Path dist = tsHome.resolve("dist");
        Path war = tsHome.resolve("dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war");
        // Validate that there are dist, src and src/vehicle.properties
        Assertions.assertTrue(Files.exists(tsHome), tsHome+" exists");
        Assertions.assertTrue(Files.exists(bin), bin+" exists");
        Assertions.assertTrue(Files.exists(classes), classes+" exists");
        Assertions.assertTrue(Files.exists(ejbliteServlet), ejbliteServlet+" exists");
        Assertions.assertTrue(Files.exists(vehicles), vehicles+" exists");
        Assertions.assertTrue(Files.exists(dist), dist+" exists");
        Assertions.assertTrue(Files.exists(war), war+" exists");
    }

    /**
     * Validate the war for the com/sun/ts/tests/ejb32/timer/service/stateless build.xml against
     * tshome/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     */
    @Test
    public void test_ejb32_timer_service_stateless_ant() {
        // vehicle = ejbliteservlet
        // app.name = ejb32_timer_service_stateless
        /*
        <ts.vehicles name="${app.name}" buildleveloverride="2" classes="
	    com/sun/ts/tests/ejb32/timer/service/common/ClientBase.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimersSingletonBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimersStatelessBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/NoTimersStatefulBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/AutoTimerBeanBase.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimerIF.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerUtil.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerBeanBase.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerBeanBaseWithoutTimeOutMethod.class,
	    com/sun/ts/tests/ejb30/timer/common/TimeoutStatusBean.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerInfo.class
	    ">
        </ts.vehicles>
         */
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/ejb32/timer/service/stateless/build.xml");
        Project project = ParseBuildTree.parseBuildXml(buildXml);
        // Needed for the ts.javac classpath by way of ts.classpath
        System.out.printf("javaee.home.ri=%s\n", project.getProperty("javaee.home.ri"));
        System.out.printf("ts.classpath=%s\n", project.getProperty("ts.classpath"));
        //
        System.out.printf("class.dir=%s\n", project.getProperty("class.dir"));
        Target pkgTarget = project.getTargets().get("package");
        Location pkgLocation = pkgTarget.getLocation();
        System.out.printf("package build.xml: %s\n", pkgLocation.getFileName());
        System.out.printf("package tasks: %s\n", Arrays.asList(pkgTarget.getTasks()));
        System.out.printf("package dependencies: %s\n", toList(pkgTarget.getDependencies()));

        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkgLocation.getFileName()));
        Assertions.assertEquals("ejbliteservlet", verifier.getVehicleSet()[0]);

        Task tsVehicle = pkgTarget.getTasks()[0];
        Hashtable<String, Object> rcAttrs = tsVehicle.getRuntimeConfigurableWrapper().getAttributeMap();
        AttributeMap attributeMap = new AttributeMap(project, rcAttrs);
        String tsVehicleName = attributeMap.getAttribute("name");
        Assertions.assertEquals("ejb32_timer_service_stateless", tsVehicleName);
        String vehiclePkgDir = project.getProperty("vehicle.pkg.dir");
        Assertions.assertEquals("com/sun/ts/tests/common/vehicle", vehiclePkgDir);

        Vehicles vehicles = new Vehicles(tsVehicleName, vehiclePkgDir, attributeMap, tsVehicle.getRuntimeConfigurableWrapper(), pkgLocation);
        System.out.println(vehicles);
        List<VehicleType> toBuild = vehicles.determineVehicleToBuild();
        Assertions.assertEquals(VehicleType.ejbliteservlet, toBuild.get(0));

        tsVehicle.perform();
        Path ejbliteVehicleClass = tsHome.resolve("classes/com/sun/ts/tests/ejb32/timer/service/stateless/EJBLiteServletVehicle.class");
        File classFile = ejbliteVehicleClass.toFile();
        // This does not exist because the ts.vehicles ejbliteservlet deletes it after building the war
        System.out.printf("ejbliteVehicleClass file: %s, exists: %s\n", classFile.getAbsolutePath(), classFile.exists());


    }
    @ArquillianResource
    Properties ctsProps;
    @Deployment
    public void test_ejb32_timer_service_stateless_ant_deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addPackage("com.ejblite.servlet");
    }

    /**
     * Validate a hand-coded shrinkwrap war against the
     * tshome/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     [starksm@scottryzen stateless]$ jar -tf $TS_HOME/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     META-INF/MANIFEST.MF
     WEB-INF/classes/com/sun/ts/lib/harness/EETest$Fault.class
     WEB-INF/classes/com/sun/ts/lib/harness/EETest$SetupException.class
     WEB-INF/classes/com/sun/ts/lib/harness/EETest.class
     WEB-INF/classes/com/sun/ts/lib/harness/ServiceEETest.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleClient.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleRunnable.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleRunnerFactory.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/ejbliteshare/EJBLiteClientIF.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/ejbliteshare/ReasonableStatus.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/helper/Helper.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/EJBLiteClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/EJBLiteJsfClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/NumberEnum.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/NumberIF.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimeoutStatusBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerBeanBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerBeanBaseWithoutTimeOutMethod.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerInfo.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerUtil.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/AutoTimerBeanBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/ClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/NoTimersStatefulBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimerIF.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimersSingletonBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimersStatelessBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/Client.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/EJBLiteServletVehicle.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/HttpServletDelegate.class
     WEB-INF/web.xml
     */
    @Test
    public void test_ejb32_timer_service_stateless_byhand() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
    }

    private List<String> toList(Enumeration<String> iter) {
        ArrayList<String> list = new ArrayList<>();
        while (iter.hasMoreElements()) {list.add(iter.nextElement());}
        return list;
    }
    private String toString(Task[] tasks) {
        StringBuilder tmp = new StringBuilder();
        tmp.append('[');
        for (Task task : tasks) {
            tmp.append(toString(task));
            tmp.append("; ");
        }
        tmp.append(']');
        return tmp.toString();
    }
    private String toString(Task task) {
        return String.format("%s, type=%s, attrs: %s", task.getTaskName(), task.getTaskType(),
                task.getRuntimeConfigurableWrapper().getAttributeMap());
    }
    private String toDotClassList(String classes) {
        return classes.replace(", ", "\n").replace('$', '.').replace('/', '.');
    }
    /**
     * An appclient test that has no vehicle
     */
    @Test
    public void test_ejb30_bb_session_stateless_basic_ant() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/ejb30/bb/session/stateless/basic/build.xml");
        Project project = ParseBuildTree.parseBuildXml(buildXml);
        // Check some project properties
        System.out.printf("app.name=%s\n", project.getProperty("app.name"));
        System.out.printf("class.dir=%s\n", project.getProperty("class.dir"));
        System.out.printf("ejb.jar.xml=%s\n", project.getProperty("ejb.jar.xml"));
        //  This is not defined at this point, it comes from the basename task in the package.ejb.jar target
        System.out.printf("ejb.jar.xml.base=%s\n", project.getProperty("ejb.jar.xml.base"));

        Target pkgTarget = project.getTargets().get("package");
        Location pkgLocation = pkgTarget.getLocation();
        System.out.printf("package build.xml: %s\n", pkgLocation.getFileName());
        System.out.printf("package tasks: %s\n", Arrays.asList(pkgTarget.getTasks()));
        System.out.printf("package dependencies: %s\n", toList(pkgTarget.getDependencies()));
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkgLocation.getFileName()));
        System.out.printf("vehicles = %s\n", Arrays.asList(verifier.getVehicleSet()));
        //Assertions.assertEquals("ejbliteservlet", verifier.getVehicleSet()[0]);

        Enumeration<String> dependencies = pkgTarget.getDependencies();
        while(dependencies.hasMoreElements()) {
            String dep = dependencies.nextElement();
            Target depTarget = project.getTargets().get(dep);
            Task[] depTasks = depTarget.getTasks();
            System.out.printf("%s, tasks: %s, dependencies: %s\n", dep, toString(depTasks), toList(depTarget.getDependencies()));
            for (Task task : depTasks) {
                System.out.printf("+++ Task(%s) children:\n", task.getTaskName());
                Enumeration<RuntimeConfigurable> children = task.getRuntimeConfigurableWrapper().getChildren();
                while (children.hasMoreElements()) {
                    RuntimeConfigurable child = children.nextElement();
                    System.out.printf("+++ +++ %s:\n", child.getElementTag());
                    Hashtable<String, Object> attrs = child.getAttributeMap();
                    AttributeMap attrMap = new AttributeMap(project, attrs);
                    for (String attr : attrs.keySet()) {
                        Object value = attrs.get(attr);
                        System.out.printf("+++ ++| --- %s: %s/%s\n+++ ++| --- --- resolved: %s\n", attr, value,
                                value.getClass(), attrMap.getAttribute(attr));
                    }
                }
            }
        }

        // package.ejb.jar target
        Target pkgEjbJar = project.getTargets().get("package.ejb.jar");
        // Need to execute the dirname, basename tasks to set properties used by the ts.ejbjar
        Task dirname = pkgEjbJar.getTasks()[0];
        dirname.maybeConfigure();
        dirname.execute();
        Task basename = pkgEjbJar.getTasks()[1];
        basename.maybeConfigure();
        basename.execute();
        // Now these properties should be set
        System.out.printf("Post dirname/basename task execution:\n");
        System.out.printf("ejb.jar.xml.dir=%s\n", project.getProperty("ejb.jar.xml.dir"));
        System.out.printf("ejb.jar.xml.base=%s\n", project.getProperty("ejb.jar.xml.base"));

        pkgLocation = pkgEjbJar.getLocation();
        System.out.printf("package.ejb.jar build.xml: %s\n", pkgLocation.getFileName());
        Task tsEjbJarTask = pkgEjbJar.getTasks()[2];
        EjbJar ejbJarDef = new EjbJar(project, tsEjbJarTask.getRuntimeConfigurableWrapper());
        System.out.println(ejbJarDef);
        String relativePath = (ejbJarDef.getRelativeDescriptorPath());
        System.out.println("ejb-jar relative path: "+relativePath);

        System.out.printf("ejb.jar.classes: %s\n", toDotClassList(project.getProperty("ejb.jar.classes")));
        System.out.printf("appclient.jar.classes: %s\n", toDotClassList(project.getProperty("appclient.jar.classes")));

        String archiveName = ejbJarDef.getArchiveName();
        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, archiveName+"_.jar");
        System.out.printf("ejbClasses: %s\n", ejbJarDef.getClassFilesString());

        STGroup ejbJarGroup = new STGroupFile("TsEjbJar.stg");
        //System.out.println(ejbJarGroup.show());
        ST genRepo = ejbJarGroup.getInstanceOf("genJar");
        genRepo.add("ejbjar", ejbJarDef);
        genRepo.add("testClass", "ClientTest");
        String ejbJarCode = genRepo.render();
        System.out.println(ejbJarCode);

        // package.appclient.jar target
        Target pkgClientJar = project.getTargets().get("package.appclient.jar");
        // Need to execute the dirname, basename tasks to set properties used by the ts.ejbjar
        Task dirname2 = pkgClientJar.getTasks()[0];
        dirname2.maybeConfigure();
        dirname2.execute();
        Task basename2 = pkgClientJar.getTasks()[1];
        basename2.maybeConfigure();
        basename2.execute();
        // Now these properties should be set
        System.out.printf("Post dirname2/basename2 task execution:\n");
        System.out.printf("application.client.xml.dir=%s\n", project.getProperty("application.client.xml.dir"));
        System.out.printf("application.client.xml.base=%s\n", project.getProperty("application.client.xml.base"));

        pkgLocation = pkgEjbJar.getLocation();
        System.out.printf("package.appclient.jar build.xml: %s\n", pkgLocation.getFileName());
        Task tsClientJarTask = pkgClientJar.getTasks()[2];
        ClientJar clientJarDef = new ClientJar(project, tsClientJarTask.getRuntimeConfigurableWrapper());
        System.out.println(clientJarDef);

        STGroup clientJarGroup = new STGroupFile("TsClientJar.stg");
        //System.out.println(ejbJarGroup.show());
        ST clientRepo = clientJarGroup.getInstanceOf("genJar");
        clientRepo.add("client", clientJarDef);
        clientRepo.add("testClass", "ClientTest");
        String clientJarCode = clientRepo.render();
        System.out.println(clientJarCode);

        Target pkgWar = project.getTargets().get("package.war");


        // package.ear target
        Target pkgEar = project.getTargets().get("package.ear");
        pkgLocation = pkgEar.getLocation();
        System.out.printf("package.ear build.xml: %s\n", pkgLocation.getFileName());
        Task tsEarTask = pkgEar.getTasks()[0];
        Ear earDef = new Ear(project, tsEarTask.getRuntimeConfigurableWrapper());
        System.out.println(earDef);
        System.out.printf("build.level: %s\n", project.getProperty("build.level"));
        System.out.printf("build.vi: %s\n", project.getProperty("build.vi"));
        System.out.printf("dist.dir: %s\n", project.getProperty("dist.dir"));
        System.out.printf("pkg.dir: %s\n", project.getProperty("pkg.dir"));
        System.out.printf("tmp.dir: %s\n", project.getProperty("tmp.dir"));
        System.out.printf("keep.archives: %s\n", project.getProperty("keep.archives"));
        System.out.printf("update: %s\n", project.getProperty("update"));


        // The contents of the ear is based on previous ts.clientjar, ts.ejbjar and ts.war tasks

    }

    // src/com/sun/ts/tests/appclient/deploy/ejblink/casesens/build.xml
    @Test
    public void test_ejblink_casesens() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/appclient/deploy/ejblink/casesens/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
    }
    // src/com/sun/ts/tests/appclient/deploy/ejblink/path/build.xml
    @Test
    public void test_ejblink_path() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/appclient/deploy/ejblink/path/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        System.out.println(pkgTarget.getEjbJarDefs());
    }

    // src/com/sun/ts/tests/ejb/ee/tx/txbean/build.xml
    @Test
    public void test_two_ears() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/appclient/deploy/ejblink/path/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        System.out.println(pkgTarget.getEjbJarDefs());
    }

    // src/com/sun/ts/tests/assembly/classpath/ejb/build.xml
    @Test
    public void test_has_jar_task() {
        // This build.xml has an extra ant and jar task to create jars that are added as libs to ts.ear outtput
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/assembly/classpath/ejb/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        System.out.println(pkgTarget.getUnhandledTaks());
    }

    // src/com/sun/ts/tests/assembly/compat/cocktail/compat12_13/build.xml
    @Test
    public void test_direct_jar_ear_usage() {
        // This build.xml does not use ts.ejbjar, ts.ear, rather it directly calls jar and ear tasks
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/assembly/compat/cocktail/compat12_13/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        System.out.println(pkgTarget.getUnhandledTaks());
    }

    // src/com/sun/ts/tests/common/connector/whitebox/annotated/build.xml
    @Test
    public void test_rar() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/common/connector/whitebox/annotated/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        // ts.javac, mkdir, delete
        System.out.println(pkgTarget.getUnhandledTaks());

    }

    @Test
    public void test_localTx_conn() {
        // src/com/sun/ts/tests/connector/localTx/connection/build.xml
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/connector/localTx/connection/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkg.getLocation().getFileName()));
        System.out.printf("Vehicles: %s\n", Arrays.asList(verifier.getVehicleSet()));

        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        // build.common.apps dependency uses ant task
        System.out.println(pkgTarget.getUnhandledTaks());
    }

    // src/com/sun/ts/tests/ejb/ee/tx/session/stateless/bm/TxN_Single/build.xml
    @Test
    public void test_ejb_ee_tx_session_stateless_bm_txn_single() {
        // src/com/sun/ts/tests/ejb/ee/tx/session/stateless/bm/TxN_Single/build.xml
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/ejb/ee/tx/session/stateless/bm/TxN_Single/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkg.getLocation().getFileName()));
        System.out.printf("Vehicles: %s\n", Arrays.asList(verifier.getVehicleSet()));

        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        // build.common.apps dependency uses ant task
        System.out.println(pkgTarget.getUnhandledTaks());
    }

    /**
     * src/com/sun/ts/tests/jms/core/bytesMsgTopic/build.xml
     * The tcxk dist artifacts:
     [starksm@scottryzen jakartaeetck]$ ls dist/com/sun/ts/tests/jms/core/bytesMsgTopic/
     bytesMsgTopic_appclient_vehicle_client.jar
     bytesMsgTopic_appclient_vehicle_client.jar.jboss-client.xml
     bytesMsgTopic_appclient_vehicle_client.jar.sun-application-client.xml
     bytesMsgTopic_appclient_vehicle.ear
     bytesMsgTopic_ejb_vehicle_client.jar
     bytesMsgTopic_ejb_vehicle_client.jar.jboss-client.xml
     bytesMsgTopic_ejb_vehicle_client.jar.sun-application-client.xml
     bytesMsgTopic_ejb_vehicle.ear
     bytesMsgTopic_ejb_vehicle_ejb.jar
     bytesMsgTopic_ejb_vehicle_ejb.jar.jboss-ejb3.xml
     bytesMsgTopic_ejb_vehicle_ejb.jar.jboss-webservices.xml
     bytesMsgTopic_ejb_vehicle_ejb.jar.sun-ejb-jar.xml
     bytesMsgTopic_jsp_vehicle.ear
     bytesMsgTopic_jsp_vehicle_web.war
     bytesMsgTopic_jsp_vehicle_web.war.jboss-webservices.xml
     bytesMsgTopic_jsp_vehicle_web.war.jboss-web.xml
     bytesMsgTopic_jsp_vehicle_web.war.sun-web.xml
     bytesMsgTopic_servlet_vehicle.ear
     bytesMsgTopic_servlet_vehicle_web.war
     bytesMsgTopic_servlet_vehicle_web.war.jboss-webservices.xml
     bytesMsgTopic_servlet_vehicle_web.war.jboss-web.xml
     bytesMsgTopic_servlet_vehicle_web.war.sun-web.xml
     */
    @Test
    public void test_jms_core_bytesMsgTopic() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/jms/core/bytesMsgTopic/build.xml");
        Project project = new Project();
        project.init();
        // Capture the ts.* tasks using a BuildListener
        project.addBuildListener(new BuildListener() {

            @Override
            public void buildStarted(BuildEvent event) {
                System.out.printf("buildStarted: %s\n", event);
            }

            @Override
            public void buildFinished(BuildEvent event) {
                System.out.printf("buildFinished: %s\n", event);
            }

            @Override
            public void targetStarted(BuildEvent event) {
                //System.out.printf("targetStarted: %s\n", event);
            }

            @Override
            public void targetFinished(BuildEvent event) {
                //System.out.printf("targetFinished: %s\n", event);
            }

            @Override
            public void taskStarted(BuildEvent event) {
                //System.out.printf("taskStarted: %s\n", event);
            }

            @Override
            public void taskFinished(BuildEvent event) {
                Task task = event.getTask();
                String name = task.getTaskName();
                if(name.startsWith("ts.")) {
                    if(name.equals("ts.verbose")) {
                        System.out.printf("ts.verbose: %s\n", task.getRuntimeConfigurableWrapper().getAttributeMap().get("message"));
                    } else {
                        System.out.printf("taskFinished: %s\n", name);
                        if (task.getTaskName().equals("ts.clientjar")) {
                            ClientJar clientJarDef = new ClientJar(project, task.getRuntimeConfigurableWrapper());
                            System.out.printf("ClientJar: %s\n", clientJarDef);
                        }
                    }
                }
            }

            @Override
            public void messageLogged(BuildEvent event) {

            }
        });
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkg.getLocation().getFileName()));
        System.out.printf("Vehicles: %s\n", Arrays.asList(verifier.getVehicleSet()));

        Task tsVehicles = pkg.getTasks()[0];
        tsVehicles.getRuntimeConfigurableWrapper().setAttribute("vehicleoverride", "appclient");
        pkg.execute();

        // Walk through the ts.vehicles task contents
        RuntimeConfigurable rc = tsVehicles.getRuntimeConfigurableWrapper();
        System.out.printf("ts.vehicles root RC: %s, attrs=%s\n", rc.getElementTag(), rc.getAttributeMap());
        for (RuntimeConfigurable rcc : Utils.asList(rc.getChildren())) {
            System.out.printf("+++ child RC: %s, attrs=%s\n", rcc.getElementTag(), rcc.getAttributeMap());
            for (RuntimeConfigurable rccc : Utils.asList(rcc.getChildren())) {
                System.out.printf("+++ +++ nested RC: %s, attrs=%s\n", rccc.getElementTag(), rccc.getAttributeMap());
            }
        }

        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        Vehicles vehiclesDef = pkgTarget.getVehiclesDef();
        System.out.printf("Vehicles: %s\n", vehiclesDef);
    }

    /**
     * Alternate test_jms_core_bytesMsgTopic using a TsTaskListener
     */
    @Test
    public void test_jms_core_bytesMsgTopic_tslistener() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/jms/core/bytesMsgTopic/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkg.getLocation().getFileName()));
        System.out.printf("Vehicles: %s\n", Arrays.asList(verifier.getVehicleSet()));

        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);

        TsTaskListener buildListener = new TsTaskListener(pkgTarget);
        project.addBuildListener(buildListener);
        Task tsVehicles = pkg.getTasks()[0];
        tsVehicles.getRuntimeConfigurableWrapper().setAttribute("vehicleoverride", "appclient");
        pkg.execute();

        System.out.println("Post package execute:");
        System.out.println(pkgTarget.toSummary());
        Assertions.assertTrue(pkgTarget.hasClientJarDef(), "Client jar definition should be present");
        Assertions.assertTrue(pkgTarget.hasEarDef(), "Ear definition should be present");
        Assertions.assertTrue(pkgTarget.hasVehiclesDef(), "Vehicles definition should be present");
        System.out.println(pkgTarget.getClientJarDef());
        System.out.println(pkgTarget.getEarDef());
        System.out.println(pkgTarget.getVehiclesDef());
    }

    // com/sun/ts/tests/jpa/core/annotations/access/field/build.xml
    @Test
    public void test_par_tslistener() {
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/jpa/core/annotations/access/field/build.xml");
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target pkg = project.getTargets().get("package");
        Assertions.assertNotNull(pkg);

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkg.getLocation().getFileName()));
        System.out.printf("Vehicles: %s\n", Arrays.asList(verifier.getVehicleSet()));

        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);

        TsTaskListener buildListener = new TsTaskListener(pkgTarget);
        project.addBuildListener(buildListener);
        Task tsVehicles = pkg.getTasks()[1];
        tsVehicles.getRuntimeConfigurableWrapper().setAttribute("vehicleoverride", "stateless3");
        pkg.execute();

        System.out.println("Post package execute:");
        System.out.println(pkgTarget.toSummary());
        Assertions.assertTrue(pkgTarget.hasParDef(), "Persistence jar definition should be present");
        Assertions.assertTrue(pkgTarget.hasEarDef(), "Ear definition should be present");
        Assertions.assertTrue(pkgTarget.hasVehiclesDef(), "Vehicles definition should be present");
        System.out.println(pkgTarget.getParDef());
        System.out.println(pkgTarget.getEarDef());
        System.out.println(pkgTarget.getVehiclesDef());
    }

    // src/com/sun/ts/tests/appclient/deploy/compat12_13/build.xml
    @Test
    public void test_x() {

    }
}
