import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

import markdoc from '@astrojs/markdoc';
import remarkInlineImage from './src/remark/inline-image.mjs';
import remarkPathOverride from './src/remark/path-override.mjs';

export default defineConfig({
    site: 'https://astroimagej.com',
    integrations: [starlight({
        title: 'AstroImageJ Docs',
        tableOfContents: { minHeadingLevel: 1, maxHeadingLevel: 4 },
        social: [
            { icon: 'github', label: 'GitHub', href: 'https://github.com/AstroImageJ/astroimagej' },
            { icon: 'comment', label: 'Github Discussions', href: 'https://github.com/AstroImageJ/astroimagej/discussions' },
            { icon: 'comment-alt', label: 'Nabble Forum', href: 'http://astroimagej.170.s1.nabble.com/' },
        ],
        editLink: {
            baseUrl: 'https://github.com/AstroImageJ/astroimagej/edit/master/website/',
        },
        components: {
            SocialIcons: './src/components/StarlightSocialIcons.astro',
        },
        customCss: ['./src/styles/inlineImage.css'],
    }), markdoc()],
    markdown: {
        remarkPlugins: [
          remarkInlineImage,
          remarkPathOverride,
        ],
    },
});