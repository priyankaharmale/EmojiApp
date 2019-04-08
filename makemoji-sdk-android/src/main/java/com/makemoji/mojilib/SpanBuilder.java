package com.makemoji.mojilib;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.makemoji.mojilib.model.MojiModel;

import org.ccil.cowan.tagsoup2.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Scott Baar on 12/3/2015.
 */
class SpanBuilder implements ContentHandler {

        private static final float[] HEADER_SIZES = {
                1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
        };

        private String mSource;
        private XMLReader mReader;
        private SpannableStringBuilder mSpannableStringBuilder;
        private Html.ImageGetter mImageGetter;
        private Html.TagHandler mTagHandler;
        private TextView refreshView;
        private ParsedAttributes parsedAttributes;
        private boolean mSimple;
        private boolean addSpaces;//add spaces to mojispans

        public SpanBuilder(
                String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler,
                Parser parser, boolean simple,TextView refreshView,boolean addSpaces) {

            mSource = source;
            mSpannableStringBuilder = new SpannableStringBuilder();
            mImageGetter = imageGetter;
            mTagHandler = tagHandler;
            mReader = parser;
            this.refreshView=refreshView;
            parsedAttributes = new ParsedAttributes();
            mSimple =simple;
            this.addSpaces = addSpaces;
        }

        public ParsedAttributes convert() {

            mReader.setContentHandler(this);
            try {
                mReader.parse(new InputSource(new StringReader(mSource)));
            } catch (IOException e) {
                // We are reading from a string. There should not be IO problems.
                throw new RuntimeException(e);
            } catch (SAXException e) {
                // TagSoup doesn't throw parse exceptions.
                throw new RuntimeException(e);
            }

            // Fix flags and range for paragraph-type markup.
            Object[] obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
            for (int i = 0; i < obj.length; i++) {
                int start = mSpannableStringBuilder.getSpanStart(obj[i]);
                int end = mSpannableStringBuilder.getSpanEnd(obj[i]);

                // If the last line of the range is blank, back off by one.
                if (end - 2 >= 0) {
                    if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
                            mSpannableStringBuilder.charAt(end - 2) == '\n') {
                        end--;
                    }
                }

                if (end == start) {
                    mSpannableStringBuilder.removeSpan(obj[i]);
                } else {
                    mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
                }
            }
            parsedAttributes.spanned = mSpannableStringBuilder;
            refreshView=null;
            return parsedAttributes;
        }

        private void handleStartTag(String tag, Attributes attributes) {
            if (tag.equalsIgnoreCase("br")) {
                // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
                // so we can safely emite the linebreaks when we handle the close tag.
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder,attributes,parsedAttributes);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder,attributes,parsedAttributes);
            } else if (tag.equalsIgnoreCase("strong")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("b")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("em")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("cite")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("dfn")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("i")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("big")) {
                start(mSpannableStringBuilder, new Big());
            } else if (tag.equalsIgnoreCase("small")) {
                start(mSpannableStringBuilder, new Small());
            } else if (tag.equalsIgnoreCase("font")) {
                startFont(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Blockquote());
            } else if (tag.equalsIgnoreCase("tt")) {
                start(mSpannableStringBuilder, new Monospace());
            } else if (tag.equalsIgnoreCase("a")) {
                startA(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("u")) {
                start(mSpannableStringBuilder, new Underline());
            } else if (tag.equalsIgnoreCase("sup")) {
                start(mSpannableStringBuilder, new Super());
            } else if (tag.equalsIgnoreCase("sub")) {
                start(mSpannableStringBuilder, new Sub());
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
            } else if (tag.equalsIgnoreCase("img")) {
                startImg(mSpannableStringBuilder, attributes,parsedAttributes, mImageGetter,refreshView,mSimple,addSpaces);
            }
            else if (tag.equalsIgnoreCase("span")){
                parseSpanTag(mSpannableStringBuilder,attributes,parsedAttributes);
            }
            else if (mTagHandler != null) {
                mTagHandler.handleTag(true, tag, mSpannableStringBuilder, mReader);
            }
        }

        private void handleEndTag(String tag) {
            if (tag.equalsIgnoreCase("br")) {
                handleBr(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("strong")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("b")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("em")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("cite")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("dfn")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("i")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("big")) {
                end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
            } else if (tag.equalsIgnoreCase("small")) {
                end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
            } else if (tag.equalsIgnoreCase("font")) {
                endFont(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan());
            } else if (tag.equalsIgnoreCase("tt")) {
                end(mSpannableStringBuilder, Monospace.class,
                        new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("a")) {
                endA(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("u")) {
                end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
            } else if (tag.equalsIgnoreCase("sup")) {
                end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            } else if (tag.equalsIgnoreCase("sub")) {
                end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                endHeader(mSpannableStringBuilder);
            } else if (mTagHandler != null) {
                mTagHandler.handleTag(false, tag, mSpannableStringBuilder, mReader);
            }
        }

    static Pattern marginT = Pattern.compile("(?:margin-top:)(\\d+)(?:.*)");
    static Pattern marginB = Pattern.compile("(?:margin-bottom:)(\\d+)(?:.*)");
    static Pattern marginL = Pattern.compile("(?:margin-left:)(\\d+)(?:.*)");
    static Pattern marginR = Pattern.compile("(?:margin-right:)(\\d+)(?:.*)");
    static Pattern fontFamily = Pattern.compile("(?:font-family:')(.*)(?:';)");
    static Pattern fontSize = Pattern.compile("(?:font-size:)(\\d+)(?:.*)");
    static Pattern colorPattern = Pattern.compile("(?:color:)(#\\d+)(?:.*)");
    private static void handleP(SpannableStringBuilder text, Attributes attributes, ParsedAttributes parsedAttributes){
        String style = attributes.getValue("","style");
        if (style!=null) {
            Matcher m = marginT.matcher(style);
            if (m.find()) parsedAttributes.marginTop = Integer.parseInt(m.group(1));
            m = marginB.matcher(style);
            if (m.find()) parsedAttributes.marginBottom = Integer.parseInt(m.group(1));
            m = marginL.matcher(style);
            if (m.find()) parsedAttributes.marginLeft = Integer.parseInt(m.group(1));
            m = marginR.matcher(style);
            if (m.find()) parsedAttributes.marginRight = Integer.parseInt(m.group(1));
            m = fontFamily.matcher(style);
            if (m.find()) parsedAttributes.fontFamily = m.group(1);
            m = fontSize.matcher(style);
            if (m.find()) parsedAttributes.fontSizePt = Integer.parseInt(m.group(1));
        }
            handleP(text);
        }
    private static void parseSpanTag(SpannableStringBuilder text, Attributes attributes, ParsedAttributes parsedAttributes){
        String style = attributes.getValue("","style");
        if (style!=null){
            Matcher m = colorPattern.matcher(style);
            try {
                if (m.find()) parsedAttributes.color = Color.parseColor(m.group(1));
            }
            catch (IllegalArgumentException e){
                Log.e("Moji SpanBuilder","Can't parse color found in html!" + e.getLocalizedMessage());
            }
        }
    }
        private static void handleP(SpannableStringBuilder text) {
            int len = text.length();

            if (len >= 1 && text.charAt(len - 1) == '\n') {
                if (len >= 2 && text.charAt(len - 2) == '\n') {
                    return;
                }

               // text.append("\n");
                return;
            }

            if (len != 0) {
               // text.append("\n\n");
            }
        }

        private static void handleBr(SpannableStringBuilder text) {
            text.append("\n");
        }

        private static Object getLast(Spanned text, Class kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
            Object[] objs = text.getSpans(0, text.length(), kind);

            if (objs.length == 0) {
                return null;
            } else {
                return objs[objs.length - 1];
            }
        }

        private static void start(SpannableStringBuilder text, Object mark) {
            int len = text.length();
            text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
        }

        private static void end(SpannableStringBuilder text, Class kind,
                                Object repl) {
            int len = text.length();
            Object obj = getLast(text, kind);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    static Pattern widthPattern = Pattern.compile("(?:width:)(\\d+)(?:.*)");
    static Pattern heightPattern = Pattern.compile("(?:height:)(\\d+)(?:.*)");
        private static void startImg(SpannableStringBuilder text,
                                     Attributes attributes, ParsedAttributes parsedAttributes, Html.ImageGetter img,TextView refreshView,
                                     boolean simple,boolean addSpaces) {
            String src = attributes.getValue("", "src");
            String style = attributes.getValue("", "style");
            String name = attributes.getValue("", "name");
            String id = attributes.getValue("", "id");
            int idInt = -1;
            String link = attributes.getValue("", "link");
            try{
                idInt = Integer.parseInt(id);
            }
            catch (Exception e){
            }
            int width = 20;
            int height = 20;
            if (style!=null){
                Matcher m = widthPattern.matcher(style);
                if (m.find()) width = Integer.parseInt(m.group(1));
                m = heightPattern.matcher(style);
                if (m.find()) height = Integer.parseInt(m.group(1));
            }
            Drawable d = null;

            if (img != null) {
                d = img.getDrawable(src);
            }

            if (d == null) {
                d = //Resources.getSystem().
                        Moji.resources.
                                //getDrawable(R.drawable.mm_unknown_image);
                                getDrawable(R.drawable.mm_placeholder);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            }

            int len = text.length();
            if (addSpaces) text.append(" \uFFFC ");
            else text.append("\uFFFC");

            final MojiSpan mojiSpan = MojiSpan.createMojiSpan(d,src,width,height,parsedAttributes.fontSizePt,simple,link,refreshView,null);
            mojiSpan.name = name;
            mojiSpan.id = idInt;
            MojiModel model = new MojiModel(name,src);
            model.id = idInt;
            model.link_url = link;
            mojiSpan.model = model;

            text.setSpan(mojiSpan, len, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mojiSpan.getLink()!=null && !mojiSpan.getLink().isEmpty()) {
               // if (refreshView!=null)refreshView.setHighlightColor(Color.TRANSPARENT);
                ClickableSpan clickableSpan = new MojiClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        HyperMojiListener hyperMojiListener = (HyperMojiListener) widget.getTag(R.id._makemoji_hypermoji_listener_tag_id);
                        if (hyperMojiListener == null)
                            hyperMojiListener = Moji.getDefaultHyperMojiClickBehavior();
                        hyperMojiListener.onClick(mojiSpan.getLink());
                    }
                };
                text.setSpan(clickableSpan, len, text.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (refreshView!=null)refreshView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        private static void startFont(SpannableStringBuilder text,
                                      Attributes attributes) {
            String color = attributes.getValue("", "color");
            String face = attributes.getValue("", "face");

            int len = text.length();
            text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
        }
    private static int getHtmlColor(String s){
        return Color.parseColor(s);
    }

        private static void endFont(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Font.class);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                Font f = (Font) obj;

                if (!TextUtils.isEmpty(f.mColor)) {
                    if (f.mColor.startsWith("@")) {
                        Resources res = Resources.getSystem();
                        String name = f.mColor.substring(1);
                        int colorRes = res.getIdentifier(name, "color", "android");
                        if (colorRes != 0) {
                            ColorStateList colors = res.getColorStateList(colorRes);
                            text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                                    where, len,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else {
                        int c = getHtmlColor(f.mColor);
                        if (c != -1) {
                            text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                                    where, len,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }

                if (f.mFace != null) {
                    text.setSpan(new TypefaceSpan(f.mFace), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static void startA(SpannableStringBuilder text, Attributes attributes) {
            String href = attributes.getValue("", "href");

            int len = text.length();
            text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
        }

        private static void endA(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Href.class);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                Href h = (Href) obj;

                if (h.mHref != null) {
                    text.setSpan(new URLSpan(h.mHref), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static void endHeader(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Header.class);

            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            // Back off not to change only the text, not the blank line.
            while (len > where && text.charAt(len - 1) == '\n') {
                len--;
            }

            if (where != len) {
                Header h = (Header) obj;

                text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]),
                        where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new StyleSpan(Typeface.BOLD),
                        where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            handleStartTag(localName, attributes);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            handleEndTag(localName);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */

            for (int i = 0; i < length; i++) {
                char c = ch[i + start];

                if (/*c == ' ' ||*/ c == '\n') {
                    char pred;
                    int len = sb.length();

                    if (len == 0) {
                        len = mSpannableStringBuilder.length();

                        if (len == 0) {
                            pred = '\n';
                        } else {
                            pred = mSpannableStringBuilder.charAt(len - 1);
                        }
                    } else {
                        pred = sb.charAt(len - 1);
                    }

                    if (pred != ' ' && pred != '\n') {
                        sb.append(' ');
                    }
                } else {
                    sb.append(c);
                }
            }

            mSpannableStringBuilder.append(sb);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        static class Bold { }
        static class Italic { }
        static class Underline { }
        static class Big { }
        static class Small { }
        static class Monospace { }
        static class Blockquote { }
        static class Super { }
        static class Sub { }

        static class Font {
            public String mColor;
            public String mFace;

            public Font(String color, String face) {
                mColor = color;
                mFace = face;
            }
        }

        static class Href {
            public String mHref;

            public Href(String href) {
                mHref = href;
            }
        }

        static class Header {
            private int mLevel;

            public Header(int level) {
                mLevel = level;
            }
        }
    }


