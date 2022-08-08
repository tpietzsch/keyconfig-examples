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
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

public class Example4_DiscoveringActions
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
	 * Manages a collection of {@link Keymap}.
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
			// not implemented. TODO
		}
	}

	public static final CommandDescriptionProvider.Scope DEMO_SCOPE = new CommandDescriptionProvider.Scope( "tpietzsch.keymap" );

	public static final String DEMO_CONTEXT = "demo";

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class MyActionDescriptions extends CommandDescriptionProvider
	{
		public MyActionDescriptions()
		{
			// The scope can be used to filter CommandDescriptionProviders
			// during discovery. The context is the context of the described
			// actions.
			super( DEMO_SCOPE, DEMO_CONTEXT );
		}

		// Every discovered CommandDescriptionProvider will be instantiated, and
		// its getCommandDescriptions() method will be called with a
		// CommandDescriptions argument to be filled.
		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( "Action A", new String[] { "SPACE" }, "trigger Action A" );
			descriptions.add( "Action B", new String[] { "B", "shift B" }, "trigger Action B" );
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

		// Discover all {@code CommandDescriptionProvider}s with scope {@code DEMO_SCOPE}.
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = new Context( PluginService.class );
		context.inject( builder );
		builder.discoverProviders( DEMO_SCOPE );
		context.dispose();

		// Alternatively, we can also manually add CommandDescriptionsProviders
		// to a CommandDescriptionsBuilder:
		//     builder.addManually( new MyActionDescriptions(), DEMO_CONTEXT );

		// After we add everything we need to the builder, we can build the
		// CommandDescriptions
		final CommandDescriptions descriptions = builder.build();

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
		actions.runnableAction( () -> preferencesDialog.setVisible( !preferencesDialog.isVisible() ),
				"Preferences",
				"meta COMMA", "ctrl COMMA" );
	}
}
