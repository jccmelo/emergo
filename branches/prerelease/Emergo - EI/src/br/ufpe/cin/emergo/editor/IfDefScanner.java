package br.ufpe.cin.emergo.editor;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class IfDefScanner extends RuleBasedScanner {

	private static Color DIRECTIVES_COLOR = new Color(Display.getCurrent(), new RGB(0, 0, 200));
	
	public IfDefScanner() {

        TextAttribute textAttribute = new TextAttribute(DIRECTIVES_COLOR);

        IToken directives = new Token(textAttribute);

        IRule[] rules = new IRule[2];

        rules[0] = new SingleLineRule("//#ifdef", " ", directives);
        rules[1] = new SingleLineRule("//#endif", "", directives);

        setRules(rules);
    }

}