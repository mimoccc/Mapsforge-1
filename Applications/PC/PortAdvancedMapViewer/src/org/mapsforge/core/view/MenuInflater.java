package org.mapsforge.core.view;

import javax.swing.JMenuBar;

import org.mapsforge.core.app.Activity;


public class MenuInflater extends JMenuBar {

	private static final long serialVersionUID = -7239994046426122877L;
	
	Activity activity;

	public MenuInflater() {
		super();
	}
	
	public MenuInflater(Activity activity) {
		super();
		this.activity = activity;
		inflate(-1, new Menu("Fake"));
	}
	/**
     * Inflate a menu hierarchy from the specified XML resource. Throws
     * {@link InflateException} if there is an error.
     *
     * @param menuRes Resource ID for an XML layout resource to load (e.g.,
     *            <code>R.menu.main_activity</code>)
     * @param menu The Menu to inflate into. The items and submenus will be
     *            added to this Menu.
     */
	/* Without XML Parser */
	public void inflate(int menuRes, Menu menu) {
		MenuItem item;
		menu = new Menu("Info");
		menu.setItemID(0x7f09000e);
		menu.setMnemonic(java.awt.event.KeyEvent.VK_I);
		menu.getAccessibleContext().setAccessibleDescription("Info");
		
		item = new MenuItem("Map file properties");
		menu.setItemID(0x7f09000f);
		item.setMnemonic(java.awt.event.KeyEvent.VK_F);
		item.getAccessibleContext().setAccessibleDescription("Map Files Properties");
		menu.add(item);
		
		item = new MenuItem("About this software");
		menu.setItemID(0x7f090010);
		item.setMnemonic(java.awt.event.KeyEvent.VK_A);
		item.getAccessibleContext().setAccessibleDescription("About this software");
		menu.add(item);
		
		this.add(menu);
		
		menu = new Menu("Position");
		menu.setItemID(0x7f090011);
		menu.setMnemonic(java.awt.event.KeyEvent.VK_P);
		menu.getAccessibleContext().setAccessibleDescription("Position");
		
		item = new MenuItem("Follow GPS signal");
		menu.setItemID(0x7f090012);
		item.setMnemonic(java.awt.event.KeyEvent.VK_G);
		item.getAccessibleContext().setAccessibleDescription("About this software");
		menu.add(item);
		
		item = new MenuItem("Enter coordinates");
		menu.setItemID(0x7f090013);
		item.setMnemonic(java.awt.event.KeyEvent.VK_E);
		item.getAccessibleContext().setAccessibleDescription("Enter coordinates");
		menu.add(item);
		
		item = new MenuItem("Map file center");
		menu.setItemID(0x7f090014);
		item.setMnemonic(java.awt.event.KeyEvent.VK_C);
		item.getAccessibleContext().setAccessibleDescription("Map file center");
		menu.add(item);
		
		this.add(menu);
		
		menu = new Menu("Screenshot");
		menu.setItemID(0x7f090015);
		menu.setMnemonic(java.awt.event.KeyEvent.VK_S);
		menu.getAccessibleContext().setAccessibleDescription("Screenshot");
		
		item = new MenuItem("JPEG (lossy)");
		menu.setItemID(0x7f090016);
		item.setMnemonic(java.awt.event.KeyEvent.VK_J);
		item.getAccessibleContext().setAccessibleDescription("JPEG (lossy)");
		menu.add(item);
		
		item = new MenuItem("PNG (lossless)");
		menu.setItemID(0x7f090017);
		item.setMnemonic(java.awt.event.KeyEvent.VK_N);
		item.getAccessibleContext().setAccessibleDescription("PNG (lossless)");
		menu.add(item);
		
		this.add(menu);
		
		
		menu = new Menu("Preferences");
		menu.setItemID(0x7f090018);
		menu.setMnemonic(java.awt.event.KeyEvent.VK_R);
		menu.getAccessibleContext().setAccessibleDescription("Preferences");
		
		//TODO
		
		this.add(menu);
		
		menu = new Menu("Map File");
		menu.setItemID(0x7f090019);
		menu.setMnemonic(java.awt.event.KeyEvent.VK_M);
		menu.getAccessibleContext().setAccessibleDescription("Map File");
		
		//TODO
		this.add(menu);
	}
	
	/* With XML Parser */
	/* public void inflate(int menuRes, Menu menu) {
		XmlResourceParser parser = null;
        try {
            parser = mContext.getResources().getLayout(menuRes);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            parseMenu(parser, attrs, menu);
        } catch (XmlPullParserException e) {
            throw new InflateException("Error inflating menu XML", e);
        } catch (IOException e) {
            throw new InflateException("Error inflating menu XML", e);
        } finally {
            if (parser != null) parser.close();
        }
	}*/
}
