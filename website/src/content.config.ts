import {defineCollection} from 'astro:content';
import {docsLoader} from '@astrojs/starlight/loaders';
import {docsSchema} from '@astrojs/starlight/schema';
import {glob} from 'astro/loaders';
import {z} from "astro/zod";

const releases = defineCollection({
	// Load Markdown files in the src/content/releases directory.
	loader: glob({ base: './src/content/releases', pattern: '**/*.md' }),
	// Type-check frontmatter using a schema
	schema: ({ image }) =>
		z.object({
			title: z.string().optional(),
			description: z.string(),
			versionNumber: z.string(),
			// Transform string to Date object
			date: z.coerce.date(),
			draft: z.coerce.boolean().default(false),
		}).transform((data) => ({
			// Generate title from version number if not provided
			...data,
			title: data.title ?? `Release ${data.versionNumber}`,
		})),
});

export const collections = {
  docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
  releases,
};