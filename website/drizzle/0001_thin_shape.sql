CREATE TABLE `admin_notifications` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`kind` text NOT NULL,
	`service_request_id` integer,
	`title` text NOT NULL,
	`body` text NOT NULL,
	`is_read` integer DEFAULT 0 NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`read_at` text,
	FOREIGN KEY (`service_request_id`) REFERENCES `service_requests`(`id`) ON UPDATE no action ON DELETE no action
);
--> statement-breakpoint
CREATE TABLE `doctor_bookings` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`booking_code` text NOT NULL,
	`doctor_id` integer NOT NULL,
	`patient_name` text NOT NULL,
	`patient_phone` text NOT NULL,
	`requested_date` text NOT NULL,
	`requested_time` text DEFAULT 'first_come' NOT NULL,
	`patient_note` text,
	`whatsapp_opt_in` integer DEFAULT 0 NOT NULL,
	`status` text DEFAULT 'pending_payment' NOT NULL,
	`receipt_file_name` text,
	`receipt_mime_type` text,
	`receipt_object_key` text,
	`admin_note` text,
	`reviewed_at` text,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`) ON UPDATE no action ON DELETE no action
);
--> statement-breakpoint
CREATE UNIQUE INDEX `doctor_bookings_booking_code_unique` ON `doctor_bookings` (`booking_code`);--> statement-breakpoint
CREATE TABLE `doctors` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`full_name` text NOT NULL,
	`specialty` text NOT NULL,
	`clinic_location` text NOT NULL,
	`city` text NOT NULL,
	`booking_cost` integer NOT NULL,
	`currency` text DEFAULT 'SDG' NOT NULL,
	`availability` text DEFAULT '' NOT NULL,
	`clinic_days` text DEFAULT '[]' NOT NULL,
	`clinic_start_time` text DEFAULT '10:00' NOT NULL,
	`clinic_end_time` text DEFAULT '14:00' NOT NULL,
	`slot_duration_minutes` integer DEFAULT 30 NOT NULL,
	`phone` text NOT NULL,
	`supervisor_name` text,
	`supervisor_phone` text,
	`payment_instructions` text NOT NULL,
	`payment_instructions_visible` integer DEFAULT 1 NOT NULL,
	`status` text DEFAULT 'active' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE TABLE `service_requests` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`request_code` text NOT NULL,
	`service_type` text NOT NULL,
	`patient_name` text NOT NULL,
	`patient_phone` text NOT NULL,
	`requested_date` text NOT NULL,
	`requested_time` text NOT NULL,
	`city` text NOT NULL,
	`neighborhood` text,
	`location_note` text,
	`service_details` text,
	`status` text DEFAULT 'pending' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `service_requests_request_code_unique` ON `service_requests` (`request_code`);