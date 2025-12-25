package com.hadroncfy.fibersync.config;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public abstract class AbstractTextRenderer<C> {
    protected abstract MutableText renderString(String s);

    public Text render(C ctx, Text template){
        MutableText ret;
        var content = template.getContent();
        if (content instanceof PlainTextContent.Literal c2){
            ret = renderString(c2.string());
        } else if (content instanceof TranslatableTextContent tc){
            Object[] args = new Object[tc.getArgs().length];
            for (int i = 0; i < args.length; i++){
                Object obj = tc.getArgs()[i];
                if (obj instanceof Text obj2){
                    obj = render(ctx, obj2);
                }
                else {
                    obj = "null";
                }
                args[i] = obj;
            }
            ret = Text.translatable(tc.getKey(), args);
        } else {
            ret = template.copyContentOnly();
        }
        ret.setStyle(renderStyle(ctx, template.getStyle()));

        for (Text t: template.getSiblings()){
            ret.append(render(ctx, t));
        }
        return ret;
    }

    private Style renderStyle(C ctx, Style style){
        Style ret = style;

        HoverEvent h = style.getHoverEvent();
        if (h != null && h instanceof HoverEvent.ShowText showText){
            Text content = showText.value();
            ret = ret.withHoverEvent(new HoverEvent.ShowText(renderString(content.getString())));
        }

        ClickEvent c = style.getClickEvent();
        if (c != null && c instanceof ClickEvent.RunCommand runCmd){
            ret = ret.withClickEvent(new ClickEvent.RunCommand(renderString(runCmd.command()).getString()));
        } else if (c != null && c instanceof ClickEvent.SuggestCommand suggestCmd){
            ret = ret.withClickEvent(new ClickEvent.SuggestCommand(renderString(suggestCmd.command()).getString()));
        } else if (c != null && c instanceof ClickEvent.OpenUrl openUrl){
            // URL doesn't need rendering
            ret = ret.withClickEvent(c);
        } else if (c != null && c instanceof ClickEvent.CopyToClipboard copyCmd){
            ret = ret.withClickEvent(new ClickEvent.CopyToClipboard(renderString(copyCmd.value()).getString()));
        }

        String i = style.getInsertion();
        if (i != null){
            ret = ret.withInsertion(renderString(i).getString());
        }

        return ret;
    }
}
