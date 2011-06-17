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
		menu = new Menu("Info");
		menu.setMnemonic(java.awt.event.KeyEvent.VK_I);
		menu.getAccessibleContext().setAccessibleDescription("Info");
		this.add(menu);
		menu = new Menu("Position");
		menu.setMnemonic(java.awt.event.KeyEvent.VK_P);
		menu.getAccessibleContext().setAccessibleDescription("Position");
		this.add(menu);
		menu = new Menu("Screenshot");
		menu.setMnemonic(java.awt.event.KeyEvent.VK_S);
		menu.getAccessibleContext().setAccessibleDescription("Screenshot");
		this.add(menu);
		menu = new Menu("Preferences");
		menu.setMnemonic(java.awt.event.KeyEvent.VK_R);
		menu.getAccessibleContext().setAccessibleDescription("Preferences");
		this.add(menu);
		menu = new Menu("Map File");
		menu.setMnemonic(java.awt.event.KeyEvent.VK_M);
		menu.getAccessibleContext().setAccessibleDescription("Map File");
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
