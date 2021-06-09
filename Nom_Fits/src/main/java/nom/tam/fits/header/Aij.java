package nom.tam.fits.header;

public enum Aij implements IFitsHeader {
    /**
     * Custom ANNOTATE key for AstroImageJ, where multiple ANNOTATE keys are allowed
     */
    ANNOTATE(SOURCE.AIJ, HDU.IMAGE, VALUE.STRING, "");

    @SuppressWarnings("CPD-START")
    private final IFitsHeader key;

    private final StandardCommentReplacement[] commentReplacements;

    private static final ThreadLocal<Class<?>> COMMENT_CONTEXT = new ThreadLocal<Class<?>>();

    Aij(SOURCE status, HDU hdu, VALUE valueType, String comment, StandardCommentReplacement... replacements) {
        this.key = new FitsHeaderImpl(name(), status, hdu, valueType, comment);
        this.commentReplacements = replacements;
    }

    @Override
    public String comment() {
        Class<?> contextClass = COMMENT_CONTEXT.get();
        if (contextClass == null) {
            contextClass = Object.class;
        }
        for (StandardCommentReplacement stdCommentReplacement : commentReplacements) {
            if (stdCommentReplacement.getContext().isAssignableFrom(contextClass)) {
                if (stdCommentReplacement.getComment() != null) {
                    return stdCommentReplacement.getComment();
                }
            }
        }
        return this.key.comment();
    }

    @Override
    public HDU hdu() {
        return this.key.hdu();
    }

    @Override
    public String key() {
        return this.key.key();
    }

    @Override
    public IFitsHeader n(int... number) {
        return this.key.n(number);
    }

    @Override
    public SOURCE status() {
        return this.key.status();
    }

    @Override
    @SuppressWarnings("CPD-END")
    public VALUE valueType() {
        return this.key.valueType();
    }

    public static void context(Class<?> clazz) {
        COMMENT_CONTEXT.set(clazz);
    }

    /**
     * scan for a comment with the specified reference key.
     *
     * @param commentKey
     *            the reference key
     * @return the comment for the reference key
     */
    public String getCommentByKey(String commentKey) {
        for (StandardCommentReplacement commentReplacement : commentReplacements) {
            if (commentReplacement.getRef().equals(commentKey)) {
                String foundcommentReplacement = commentReplacement.getComment();
                if (foundcommentReplacement == null) {
                    return comment();
                } else {
                    return foundcommentReplacement;
                }
            }
        }
        return null;
    }

    /**
     * set the comment for the specified reference key.
     *
     * @param commentKey
     *            the reference key
     * @param value
     *            the comment to set when the fits key is used.
     */
    public void setCommentByKey(String commentKey, String value) {
        for (StandardCommentReplacement commentReplacement : commentReplacements) {
            if (commentReplacement.getRef().equals(commentKey)) {
                commentReplacement.setComment(value);
                return;
            }
        }
    }

    private static StandardCommentReplacement replaceable(String string, Class<?> clazz) {
        return new StandardCommentReplacement(string, clazz);
    }

    private static StandardCommentReplacement replaceable(String string, Class<?> clazz, String comment) {
        return new StandardCommentReplacement(string, clazz, comment);
    }
}
