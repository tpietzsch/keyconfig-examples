package tpietzsch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

public class Example1_SettingUpActions
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

	public static void main( String[] args )
	{
		// Create a `MainPanel` and show it in a `JFrame`.
		// The `MainPanel` just has a single `JLabel` displaying the text "hello".
		// The displayed text can be changed by `MainPanel.setText(String)`.
		final JFrame frame = new JFrame( "Keymaps Demo" );
		final MainPanel mainPanel = new MainPanel();
		frame.add( mainPanel );
		frame.setPreferredSize( new Dimension( 200, 100 ) );
		frame.pack();
		frame.setVisible( true );


		// `InputActionBindings` bind inputs to actions.
		// This is of course exactly what AWT/Swing's [Key Bindinggs](https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html) framework (`InputMap`, `ActionMap`) does.
		// `InputActionBindings` adds very little over that; basically only more convenient `InputMap` chaining.
		final InputActionBindings bindings = new InputActionBindings();

		// (Side note: The initial idea of `ui-behaviour` was to offer a similar framework for mouse clicks, scrolls, drags, etc.
		//  Modeled after `InputMap` and `ActionMap`, there are `InputTriggerMap` and `BehaviourMap`.
		//  Analogous to `InputActionBindings` there is `TriggerBehaviourBindings`.)

		// We connect the `InputActionBindings` instance to our `MainPanel` as follows.
		SwingUtilities.replaceUIActionMap( mainPanel, bindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

		// `InputActionBindings` manages a chain of `InputMap`/`ActionMap` pairs.
		// Next we create a new `Actions` object, which manages one such pair (the arguments don't matter for now) ...
		final Actions actions = new Actions( new InputTriggerConfig(), "demo" );
		// ... and we add the pair to our `InputActionBindings` under the name "actions".
		actions.install( bindings, "actions" );
		// (We could use the name later to remove or temporarily block the `InputMap`/`ActionMap` pair.)


		// Now, finally, we can use the `Actions` instance to add new shortcuts:
		actions.runnableAction( () -> mainPanel.setText( "Action A triggered" ),
				"Action A",
				"SPACE", "A" );
		actions.runnableAction( () -> mainPanel.setText( "Action B triggered" ),
				"Action B",
				"B", "shift B" );
		actions.runnableAction( () -> mainPanel.setText( "Action C triggered" ),
				"Action C",
				"1", "2", "3", "4", "5", "6", "7", "8", "9", "0" );

		// The `actions.runnableAction` method takes the following arguments
		// public void runnableAction( final Runnable runnable, final String name, final String... defaultKeyStrokes )
		// A Runnable to run when the action is triggered.
		// A unique name for the action (this will be used as the actions key in the underlying `InputMap`/`ActionMap`.
		// Finally, one or more keystrokes that should trigger the action.

		// Here for example, the Runnable sets the text "Action A triggered" in the panel label.
		// It is added under the name "Action A".
		// It is triggered by the "SPACE" key, or the "A" key by default.
	}
}

