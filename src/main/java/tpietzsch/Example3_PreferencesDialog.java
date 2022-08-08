package tpietzsch;

import bdv.ui.keymap.AbstractKeymapManager;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapSettingsPage;
import bdv.ui.settings.SettingsPage;
import bdv.ui.settings.SettingsPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

public class Example3_PreferencesDialog
{
	public static class MainPanel extends JPanel
	{
		private final JLabel label;

		public MainPanel()
		{
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder( 0, 20, 0, 0 ) );
			setFocusable( true );

			label = new JLabel( "hello" );
			add( label, BorderLayout.CENTER );
		}

		public void setText( final String text )
		{
			label.setText( text );
		}
	}



	// PrefererencesDialog containing only a SettingsPanel, and addPage() method
	// to add new preference sections.
	//
	// SettingsPanel implements a typical Preferences layout (like it's used in
	// Eclipse, for example) with a tree of preferences sections on the left,
	// the selected section on the right, and Apply, Ok, Cancel buttons on the
	// bottom.
	public static class PreferencesDialog extends JDialog
	{
		private final SettingsPanel settingsPanel;

		public PreferencesDialog( final Frame owner )
		{
			super( owner, "Preferences", false );
			settingsPanel = new SettingsPanel();
			settingsPanel.onOk( () -> setVisible( false ) );
			settingsPanel.onCancel( () -> setVisible( false ) );

			setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
			addWindowListener( new WindowAdapter()
			{
				@Override
				public void windowClosing( final WindowEvent e )
				{
					settingsPanel.cancel();
				}
			} );

			getContentPane().add( settingsPanel, BorderLayout.CENTER );
			pack();
		}

		public void addPage( final SettingsPage page )
		{
			settingsPanel.addPage( page );
			pack();
		}
	}

	private static Keymap defaultKeymap;

	/**
	 * Manages a collection of {@link Keymap}. The only thing we need to add to
	 * the base class {@code AbstractKeymapManager} is providing one or more
	 * default {@code Keymap}s.
	 */
	public static class KeymapManager extends AbstractKeymapManager< KeymapManager >
	{
		@Override
		protected List< Keymap > loadBuiltinStyles()
		{
			return Collections.singletonList( defaultKeymap );
		}

		@Override
		public void saveStyles()
		{
			// Not implemented.
			// Here we would save user defined keymaps to YAML files, for example.
		}
	}

	public static void main( String[] args ) throws IOException
	{
		final JFrame frame = new JFrame( "Keymaps Demo" );
		final MainPanel mainPanel = new MainPanel();
		frame.add( mainPanel );
		frame.setPreferredSize( new Dimension( 200, 100 ) );
		frame.pack();
		frame.setVisible( true );

		final InputActionBindings bindings = new InputActionBindings();

		SwingUtilities.replaceUIActionMap( mainPanel, bindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

		// We need to supply the KeymapSettingsPage with a list of existing
		// actions, with short textual descriptions. This is done by creating a
		// CommandDescriptions object and adding the configurable actions.
		final CommandDescriptions descriptions = new CommandDescriptions();
		descriptions.setKeyconfigContext( "demo" );
		descriptions.add( "Action A", new String[] { "SPACE" }, "trigger Action A" );
		descriptions.add( "Action B", new String[] { "B", "shift B" }, "trigger Action B" );

		// Build a default Keymap from the descriptions.
		//
		// Keymap is a simple container for an InputTriggerConfig, adding just a
		// name and support for listeners to be notified when the
		// InputTriggerConfig changes.
		defaultKeymap = new Keymap( "Default", descriptions.createDefaultKeyconfig() );

		// Create a KeymapManager
		final KeymapManager keymapManager = new KeymapManager();

		// Create a PreferencesDialog
		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame );
		// Add a configuration page for the KeymapManager
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, new KeymapManager(), descriptions ) );

		// The KeyMapManager (via its base class) exposes the user-selected
		// keymap. We use that as the InputTriggerConfig for our Actions. We
		// also add a listener that refreshes the Actions keybinding when that
		// user-selected keymap changes.
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		final Actions actions = new Actions( keymap.getConfig(), "demo" );
		actions.install( bindings, "actions" );
		keymap.updateListeners().add( () -> actions.updateKeyConfig( keymap.getConfig(), false ) );


		actions.runnableAction( () -> mainPanel.setText( "Action A triggered" ),
				"Action A",
				"SPACE" );
		actions.runnableAction( () -> mainPanel.setText( "Action B triggered" ),
				"Action B",
				"B", "shift B" );

		// Add an action to show/hide the PreferencesDialog.
		actions.runnableAction( () -> preferencesDialog.setVisible( !preferencesDialog.isVisible() ),
				"Preferences",
				"meta COMMA", "ctrl COMMA" );
	}
}
