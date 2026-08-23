CREATE TABLE `provider_applications` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`profession` text NOT NULL,
	`full_name` text NOT NULL,
	`phone` text NOT NULL,
	`specialty` text,
	`experience` text,
	`city` text NOT NULL,
	`workplace` text,
	`availability` text,
	`status` text DEFAULT 'new' NOT NULL,
	`admin_note` text,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE TABLE `provider_documents` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`application_id` integer NOT NULL,
	`document_type` text NOT NULL,
	`file_name` text NOT NULL,
	`mime_type` text NOT NULL,
	`object_key` text NOT NULL,
	`review_status` text DEFAULT 'pending' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`application_id`) REFERENCES `provider_applications`(`id`) ON UPDATE no action ON DELETE no action
);
