package tpietzsch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

public class Example2_UsingConfigFiles
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


		// Another goal of ui-behaviour is to make mouse and key bindings easily
		// configurable *by the user* (for example through config files). This
		// is the purpose of the `Actions` constructor arguments.
		// `new Actions( new InputTriggerConfig(), "demo" );`
		// The first argument is a `InputTriggerConfig`, and after that one or
		// more `String` contexts are given (more on that later).
		//
		// The `InputTriggerConfig` contains is basically a map from action names to key bindings.
		// When adding a new action, for example like this:
		// ```
		// actions.runnableAction( () -> mainPanel.setText( "Action B triggered" ),
		//		"Action B",
		//		"B", "shift B" );
		// ```
		// then `actions` will first look into its `InputTriggerConfig` to check
		// whether any key binding is associated with the respective action name
		// ("Action B" in this example). If nothing is defined in the
		// `InputTriggerConfig` then (and only then) the specified
		// default key bindings will be used (`"B"` and `"shift B"` in this example).
		//
		// So far, we just used a new, empty `InputTriggerConfig`, meaning we
		// just get the specified defaults, which is exactly what we want for
		// prototyping.
		//
		// If the project becomes more mature, and we want to change the config
		// from outside, we can load the `InputTriggerConfig` from a config
		// file.
		final InputTriggerConfig config;
		try ( Reader reader = new InputStreamReader( Example2_UsingConfigFiles.class.getResourceAsStream( "config.yaml" ) ) )
		{
			config = new InputTriggerConfig( YamlConfigIO.read( reader ) );
		}

		// The `config.yaml` file looks like this:
		// ```
		// ---
		// - !mapping
		// action: Action A
		// contexts: [demo]
		// triggers: [SPACE, A]
		// - !mapping
		// action: Action B
		// contexts: [demo]
		// triggers: [N]
		// ```
		// The format should be more or less self-explanatory. The
		// `InputTriggerConfig config` should now map the String `"Action A"` to
		// the Set of Strings `{"SPACE", "A"}`, and `"Action B"` to `{"N"}`.

		final Actions actions = new Actions( new InputTriggerConfig(), "demo" );
		actions.install( bindings, "actions" );

		// We add the same actions as in the previous example:
		actions.runnableAction( () -> mainPanel.setText( "Action A triggered" ),
				"Action A",
				"SPACE" );
		actions.runnableAction( () -> mainPanel.setText( "Action B triggered" ),
				"Action B",
				"B", "shift B" );
		actions.runnableAction( () -> mainPanel.setText( "Action C triggered" ),
				"Action C",
				"1", "2", "3", "4", "5", "6", "7", "8", "9", "0" );

		// The `config` contains bindings for "Action A" and "Action B", these
		// will override the specified default bindings.
		// So "Action A" will be triggered by the "SPACE" or "A" keys, and
		// "Action B" will be triggered by "N".
		// The `config` doesn't specify anything for "Action C", so it will be
		// triggered by the programmatically specified defaults, that is, "1",
		// "2", etc.

		// Coming back to the `String... context` argument(s) of the `Actions`
		// constructor:
		//
		// The idea is that the same action (or at least action name) might
		// occur in different contexts, that is, different tools, different
		// windows of the same tool, etc. For example, an action named "Undo"
		// could occur in many contexts and it would be nice to be able to
		// assign different shortcuts, depending on context.
		//
		// Therefore, `InputTriggerConfig` does not directly map `action` to
		// shortcuts, but rather maps `(action, context)` pairs to shortcuts,
		// where `action` and `context` are both `String`s. So, for example,
		// ``("Undo", "bdv")`` can map to a different shortcut than `("Undo",
		// "paintera")`.
		//
		// The `context` arguments given in the `Actions` constructor specify
		// which subsets of key bindings defined in the `InputTriggerConfig`
		// should be considered. In the example, we have
		// `final Actions actions = new Actions( config, "demo" );`
		// So, `actions` would pick up bindings for `("Undo", "demo")` but not
		// ("Undo", "bdv") for example.


		// Finally, there is a special trigger `"not mapped"` that can be used
		// to specify that a particular action should not be associated to any
		// shortcut.
		// For example, if we add
		// ```
		// - !mapping
		// action: Action C
		// contexts: [demo]
		// triggers: [not mapped]
		// ```
		// to the `config.yaml` file, then "Action C" will be disabled
		// effectively, that is, the programmatic defaults "1", "2", etc., will
		// not be used.
	}
}

