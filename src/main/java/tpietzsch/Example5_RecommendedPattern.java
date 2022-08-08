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

public class Example5_RecommendedPattern
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

	/**
	 * Manages a collection of {@link Keymap}.
	 */
	public static class KeymapManager extends AbstractKeymapManager< KeymapManager >
	{
		private static final CommandDescriptions descriptions;

		private static List< Keymap > builtin;

		static {
			// Discover all {@code CommandDescriptionProvider}s with scope {@code DEMO_SCOPE}.
			final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
			final Context context = new Context( PluginService.class );
			context.inject( builder );
			builder.discoverProviders( DEMO_SCOPE );
			context.dispose();
			descriptions = builder.build();
			builtin = Collections.singletonList( new Keymap( "Default", descriptions.createDefaultKeyconfig() ) );
		}

		@Override
		protected List< Keymap > loadBuiltinStyles()
		{
			return builtin;
		}

		@Override
		public void saveStyles()
		{
			// not implemented. TODO
		}

		public CommandDescriptions getCommandDescriptions()
		{
			return descriptions;
		}
	}










	// Every library / tool should have a separate scope. Ideally, the scope
	// should be defined public static somewhere so that it can easily used
	// outside the component to discover its actions. For example, BigDataViewer
	// uses this scope. If another tool (BigStitcher, BigWarp, etc.) wants to
	// include BDV shortcuts into its customizable keymaps, they can be easily
	// discovered like that.
	public static final CommandDescriptionProvider.Scope DEMO_SCOPE = new CommandDescriptionProvider.Scope( "tpietzsch.keymap" );

	public static final String DEMO_CONTEXT = "demo";

	// Related actions are collected into a class MyActions.
	public static class MyActions
	{
		// Action names and default shortcuts are defined as constants, because
		// they are used both for defining the actions, and for Descriptions.

		public static final String ACTION_A = "Action A";
		public static final String[] ACTION_A_KEYS = { "SPACE" };

		public static final String ACTION_B = "Action B";
		public static final String[] ACTION_B_KEYS = { "B", "shift B" };

		public static final String PREFERENCES = "Preferences";
		public static final String[] PREFERENCES_KEYS = { "meta COMMA", "ctrl COMMA" };

		// By convention. akk actions defined in MyActions are described in a
		// static inner class named "Descriptions" (@Plugin annotated subclass
		// of CommandDescriptionsProvider).
		/*
		 * Command descriptions for all provided commands
		 */
		@Plugin( type = CommandDescriptionProvider.class )
		public static class Descriptions extends CommandDescriptionProvider
		{
			public Descriptions()
			{
				super( DEMO_SCOPE, DEMO_CONTEXT );
			}

			@Override
			public void getCommandDescriptions( final CommandDescriptions descriptions )
			{
				descriptions.add( ACTION_A, ACTION_A_KEYS, "trigger Action A" );
				descriptions.add( ACTION_B, ACTION_B_KEYS, "trigger Action B" );
				descriptions.add( PREFERENCES, PREFERENCES_KEYS, "Show the Preferences dialog." );
			}
		}

		/**
		 * Install into the specified {@link Actions}.
		 */
		public static void install( final Actions actions, final MainPanel mainPanel, final PreferencesDialog preferencesDialog )
		{
			actions.runnableAction( () -> mainPanel.setText( "Action A triggered" ),
					ACTION_A, ACTION_A_KEYS );
			actions.runnableAction( () -> mainPanel.setText( "Action B triggered" ),
					ACTION_B, ACTION_B_KEYS );
			actions.runnableAction( () -> preferencesDialog.setVisible( !preferencesDialog.isVisible() ),
					PREFERENCES, PREFERENCES_KEYS );
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

		final KeymapManager keymapManager = new KeymapManager();
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		final Actions actions = new Actions( keymap.getConfig(), DEMO_CONTEXT );
		actions.install( bindings, "actions" );
		keymap.updateListeners().add( () -> actions.updateKeyConfig( keymap.getConfig(), false ) );

		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame );
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, new KeymapManager(), keymapManager.getCommandDescriptions() ) );

		MyActions.install( actions, mainPanel, preferencesDialog );
	}
}
