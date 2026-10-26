package com.divudi.core.data;

import java.util.Locale;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.TextAlignment;
import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.codabar.CodabarBean;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.impl.int2of5.Interleaved2Of5Bean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.impl.upcean.EAN8Bean;
import org.krysalis.barcode4j.impl.upcean.UPCABean;
import org.krysalis.barcode4j.impl.upcean.UPCEBean;
import org.krysalis.barcode4j.output.AbstractCanvasProvider;

/**
 * Renders a linear barcode as inline SVG that fills exactly the given
 * width x height in mm.
 *
 * PrimeFaces {@code p:barcode format="svg"} emits an SVG with a fixed
 * viewBox, so the browser keeps its natural aspect ratio and centres it in
 * the img box: the configured width never stretches the bars. Here the bars
 * are drawn with {@code preserveAspectRatio="none"}, so they scale uniformly
 * to the configured size while keeping their relative widths.
 */
public final class PvcCardBarcodeSvg {

    private PvcCardBarcodeSvg() {
    }

    /**
     * @return the SVG markup, or {@code null} when the type is not a
     * supported linear barcode (e.g. qr) or the value cannot be encoded by
     * that type - callers then fall back to {@code p:barcode}.
     */
    public static String render(String type, String value, double widthMm, double heightMm) {
        if (value == null || value.trim().isEmpty() || widthMm <= 0 || heightMm <= 0) {
            return null;
        }
        AbstractBarcodeBean bean = beanFor(type);
        if (bean == null) {
            return null;
        }
        bean.setMsgPosition(HumanReadablePlacement.HRP_NONE);
        bean.doQuietZone(false);
        bean.setBarHeight(heightMm);
        try {
            SvgCanvas canvas = new SvgCanvas();
            bean.generateBarcode(canvas, value);
            BarcodeDimension dim = canvas.getDimensions();
            if (dim == null || canvas.rects.length() == 0) {
                return null;
            }
            return String.format(Locale.ROOT,
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fmm\" height=\"%.2fmm\" "
                    + "viewBox=\"0 0 %.4f %.4f\" preserveAspectRatio=\"none\" style=\"display:block;\">"
                    + "<g fill=\"#000\">%s</g></svg>",
                    widthMm, heightMm, dim.getWidthPlusQuiet(), dim.getHeightPlusQuiet(), canvas.rects);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static AbstractBarcodeBean beanFor(String type) {
        if (type == null) {
            return null;
        }
        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "code128":
                return new Code128Bean();
            case "code39":
                return new Code39Bean();
            case "int2of5":
                return new Interleaved2Of5Bean();
            case "codabar":
                return new CodabarBean();
            case "ean13":
                return new EAN13Bean();
            case "ean8":
                return new EAN8Bean();
            case "upca":
                return new UPCABean();
            case "upce":
                return new UPCEBean();
            default:
                return null;
        }
    }

    private static final class SvgCanvas extends AbstractCanvasProvider {

        private final StringBuilder rects = new StringBuilder();

        SvgCanvas() {
            super(0);
        }

        @Override
        public void deviceFillRect(double x, double y, double w, double h) {
            rects.append(String.format(Locale.ROOT,
                    "<rect x=\"%.4f\" y=\"%.4f\" width=\"%.4f\" height=\"%.4f\"/>", x, y, w, h));
        }

        @Override
        public void deviceText(String text, double x1, double x2, double y1, String fontName,
                double fontSize, TextAlignment textAlign) {
            // human-readable text is disabled (HRP_NONE); the card has its own PHN slot
        }
    }
}
