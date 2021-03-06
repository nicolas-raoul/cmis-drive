package sho.cmis.drive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liferay.nativity.control.MessageListener;
import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.control.NativityMessage;
import com.liferay.nativity.listeners.SocketCloseListener;
import com.liferay.nativity.listeners.SocketOpenListener;
import com.liferay.nativity.modules.contextmenu.ContextMenuControl;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlCallback;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuItem;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;

import co.paralleluniverse.javafs.JavaFS;
import sho.cmis.fs.CmisConfig;
import sho.cmis.fs.CmisFS;

@Component(immediate = true)
public class CmisDrive
{
	private static final Logger LOG = LoggerFactory.getLogger(CmisDrive.class.getName());

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private static final String ALFRESCO_URL =
		"https://cmis.alfresco.com/alfresco/api/-default-/public/cmis/versions/1.1/browser";

	private static final String LOCAL_URL = "http://localhost:8083/cmisBrowser";

	private static final String COMPONENT_NAME = "sho.cmis.drive";

	private static final String OVERLAY_OMN_ICON = "/Volumes/Shorty_JetDrive_1/tmp/omnIcon.icns";
	// private static final String OVERLAY_CMIS_ICON = "./img/cmis-wb-icon.png.icns";
	private static final String OVERLAY_CMIS_ICON = System.getProperty("icon");

	// | private static final String MOUNT_POINT = "/Volumes/Shorty_JetDrive_1/tmp/drive_mountpoint";
	private static final String MOUNT_POINT = "/Volumes/Shorty_JetDrive_1/mount";
	// private static final String CMIS_MOUNT_POINT_URI = "cmis:///Volumes/Shorty_JetDrive_1/tmp/drive_mountpoint";
	private static final String CMIS_URI = "cmis:///";

	private static final boolean READONLY = true;

	@Activate
	public void activate()
	{
		LOG.info("Activating component: {} ...", COMPONENT_NAME);

		// we need to wait for the CMIS Filesystem Bundle to be started
		Dictionary<String, Object> ht = new Hashtable<String, Object>();
		ht.put(EventConstants.EVENT_TOPIC, "org/osgi/framework/BundleEvent/STARTED");
		context.registerService(EventHandler.class.getName(), new EventHandler()
		{
			@Override
			public void handleEvent(Event event)
			{
				System.out.println("handleEvent: " + event);
				if (event.containsProperty("bundle.symbolicName"))
				{
					if (event.getProperty("bundle.symbolicName").equals("sho.cmis.fs"))
					{
						try
						{
							javafs();
							nativity();
						}
						catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}, ht);
		LOG.debug("Activated component: {}", COMPONENT_NAME);
	}

	private void javafs() throws Exception
	{

		ServiceLoader<FileSystemProvider> load = java.util.ServiceLoader.load(FileSystemProvider.class);
		for (FileSystemProvider fileSystemProvider : load)
		{
			LOG.info("ServiceLoader FSP: " + fileSystemProvider.getScheme());
		}

		for (FileSystemProvider fsr : FileSystemProvider.installedProviders())
		{
			LOG.info("FSP: " + fsr.getScheme());
		}

		Map<String, String> config = new HashMap<>();
		config.put(CmisConfig.BROWSER_URL, ALFRESCO_URL);
		// config.put(CmisConfig.BROWSER_URL, LOCAL_URL);
		config.put(CmisConfig.BINDING_TYPE, "browser");
		config.put(CmisConfig.USER, "admin");
		config.put(CmisConfig.PASSWORD, "admin");
		// config.put(CmisConfig.USER, "test");
		// config.put(CmisConfig.PASSWORD, "test");
		config.put(CmisConfig.REPOSITORY_ID, "default");
		config.put(CmisConfig.CACHE_PATH_OMIT, "false");

		FileSystem fs = CmisFS.newFileSystem(new URI(CMIS_URI), config);
		// FileSystem fs = Jimfs.newFileSystem();
		LOG.info("FS separator: " + fs.getSeparator());

		Map<String, String> options = new HashMap<>();
		// options.put("fsname", fs.getClass().getSimpleName() + "@" + System.currentTimeMillis());
		options.put("fsname", "CMIS");
		options.put("volname", "Alfresco CMIS Server");
		options.put("volicon", OVERLAY_CMIS_ICON);

		LOG.info("Mounting FileSystem ...");
		JavaFS.mount(fs, Paths.get(MOUNT_POINT), READONLY, false, options);
		LOG.info("... mounted FileSystem!");
	}

	private void nativity()
	{
		// final String testFolder = "/Volumes/Shorty_JetDrive_1/tmp";
		final String testFolder = MOUNT_POINT;

		try
		{
			final NativityControl nativityControl = NativityControlUtil.getNativityControl();

			nativityControl.addSocketOpenListener(new SocketOpenListener()
			{

				@Override
				public void onSocketOpen()
				{
					// TODO Auto-generated method stub
					System.out.println("Socket opened !!!!");

					// nativityControl.addFavoritesPath(testFolder);

					// Setting filter folders is required for Mac's Finder Sync plugin
					nativityControl.setFilterFolder(testFolder);

					final int testIconId = 98786;

					// FileIconControlCallback used by Windows and Mac
					FileIconControlCallback fileIconControlCallback = new FileIconControlCallback()
					{
						@Override
						public int getIconForFile(String path)
						{
							// return testIconId;
							System.out.println("Get ICon For File: " + path);
							return testIconId;
						}
					};

					FileIconControl fileIconControl =
						FileIconControlUtil.getFileIconControl(nativityControl, fileIconControlCallback);

					fileIconControl.enableFileIcons();

					// String testFilePath = testFolder + "/squirrel.zip";

					// if (OSDetector.isWindows())
					// {
					// // This id is determined when building the DLL
					// testIconId = 1;
					// }
					// else if (OSDetector.isMinimumAppleVersion(OSDetector.MAC_YOSEMITE_10_10))
					// {
					// Used by Mac Finder Sync. This unique id can be set at runtime.
					// testIconId = 1;

					fileIconControl.registerIconWithId(OVERLAY_CMIS_ICON, "omn", Integer.toString(testIconId));
					// }
					// else if (false)
					// {
					// // Used by Mac Injector and Linux
					// testIconId = fileIconControl.registerIcon("/Users/liferay/Desktop/testIcon.icns");
					// }
				}
			});

			nativityControl.addSocketCloseListener(new SocketCloseListener()
			{

				@Override
				public void onSocketClose()
				{
					// TODO Auto-generated method stub
					System.out.println("Socket Closed .....");
				}
			});

			nativityControl.connect();

			nativityControl.addFavoritesPath(testFolder);

			// Setting filter folders is required for Mac's Finder Sync plugin
			nativityControl.setFilterFolder(testFolder);

			/* File Icons */

			nativityControl.registerMessageListener(new MessageListener(testFolder)
			{

				@Override
				public NativityMessage onMessage(NativityMessage nativityMessage)
				{
					System.out.println("Received Message: " + nativityMessage);
					return null;
				}
			});

			final int testIconId = 98786;

			// FileIconControlCallback used by Windows and Mac
			FileIconControlCallback fileIconControlCallback = new FileIconControlCallback()
			{
				@Override
				public int getIconForFile(String path)
				{
					// return testIconId;
					System.out.println("Get ICon For File: " + path);
					return testIconId;
				}
			};

			FileIconControl fileIconControl = FileIconControlUtil.getFileIconControl(nativityControl, fileIconControlCallback);

			fileIconControl.enableFileIcons();

			// String testFilePath = testFolder + "/squirrel.zip";

			// if (OSDetector.isWindows())
			// {
			// // This id is determined when building the DLL
			// testIconId = 1;
			// }
			// else if (OSDetector.isMinimumAppleVersion(OSDetector.MAC_YOSEMITE_10_10))
			// {
			// Used by Mac Finder Sync. This unique id can be set at runtime.
			// testIconId = 1;

			fileIconControl.registerIconWithId(OVERLAY_CMIS_ICON, "omn", Integer.toString(testIconId));
			// }
			// else if (false)
			// {
			// // Used by Mac Injector and Linux
			// testIconId = fileIconControl.registerIcon("/Users/liferay/Desktop/testIcon.icns");
			// }

			// FileIconControl.setFileIcon() method only used by Linux
			// fileIconControl.setFileIcon(testFilePath, testIconId);

			/* Context Menus */

			ContextMenuControlCallback contextMenuControlCallback = new ContextMenuControlCallback()
			{
				@Override
				public List<ContextMenuItem> getContextMenuItems(String[] paths)
				{
					ContextMenuItem contextMenuItem = new ContextMenuItem("CmisDrive Menu Test");

					ContextMenuAction contextMenuAction = new ContextMenuAction()
					{
						@Override
						public void onSelection(String[] paths)
						{
							for (String path : paths)
							{
								System.out.print(path + ", ");
							}

							System.out.println("selected");
						}
					};

					contextMenuItem.setContextMenuAction(contextMenuAction);
					contextMenuItem.setIconPath(OVERLAY_CMIS_ICON);

					List<ContextMenuItem> contextMenuItems = new ArrayList<ContextMenuItem>()
					{
					};
					contextMenuItems.add(contextMenuItem);

					// Mac Finder Sync will only show the parent level of context menus
					return contextMenuItems;
				}
			};

			ContextMenuControl contextMenuControl =
				ContextMenuControlUtil.getContextMenuControl(nativityControl, contextMenuControlCallback);

			Set<String> allObservedFolders = nativityControl.getAllObservedFolders();
			for (String folder : allObservedFolders)
			{
				System.out.println("OF: " + folder);
			}
			if (allObservedFolders.isEmpty())
				System.out.println("OF is EMPTY!!!!");
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e);
			LOG.error("Error: ", e);
		}
	}

}
