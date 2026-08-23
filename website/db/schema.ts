import { integer, pgTable, serial, text, timestamp } from "drizzle-orm/pg-core";

const createdAt = () => timestamp("created_at", { withTimezone: true, mode: "string" }).notNull().defaultNow();
const updatedAt = () => timestamp("updated_at", { withTimezone: true, mode: "string" }).notNull().defaultNow();

export const providerApplications = pgTable("provider_applications", {
  id: serial("id").primaryKey(),
  profession: text("profession", { enum: ["doctor", "lab", "physio", "nurse"] }).notNull(),
  fullName: text("full_name").notNull(),
  phone: text("phone").notNull(),
  specialty: text("specialty"),
  experience: text("experience"),
  city: text("city").notNull(),
  workplace: text("workplace"),
  availability: text("availability"),
  status: text("status", { enum: ["new", "under_review", "needs_documents", "verified", "rejected"] }).notNull().default("new"),
  adminNote: text("admin_note"),
  createdAt: createdAt(),
  updatedAt: updatedAt(),
});

export const providerDocuments = pgTable("provider_documents", {
  id: serial("id").primaryKey(),
  applicationId: integer("application_id").notNull().references(() => providerApplications.id),
  documentType: text("document_type", { enum: ["cv", "qualification", "license", "identity"] }).notNull(),
  fileName: text("file_name").notNull(),
  mimeType: text("mime_type").notNull(),
  objectKey: text("object_key").notNull(),
  reviewStatus: text("review_status", { enum: ["pending", "accepted", "rejected"] }).notNull().default("pending"),
  createdAt: createdAt(),
});

export const doctors = pgTable("doctors", {
  id: serial("id").primaryKey(),
  fullName: text("full_name").notNull(),
  specialty: text("specialty").notNull(),
  clinicLocation: text("clinic_location").notNull(),
  city: text("city").notNull(),
  bookingCost: integer("booking_cost").notNull(),
  currency: text("currency").notNull().default("SDG"),
  availability: text("availability").notNull().default(""),
  clinicDays: text("clinic_days").notNull().default("[]"),
  clinicStartTime: text("clinic_start_time").notNull().default("10:00"),
  clinicEndTime: text("clinic_end_time").notNull().default("14:00"),
  slotDurationMinutes: integer("slot_duration_minutes").notNull().default(30),
  phone: text("phone").notNull(),
  supervisorName: text("supervisor_name"),
  supervisorPhone: text("supervisor_phone"),
  paymentInstructions: text("payment_instructions").notNull(),
  paymentInstructionsVisible: integer("payment_instructions_visible").notNull().default(1),
  status: text("status", { enum: ["active", "inactive"] }).notNull().default("active"),
  createdAt: createdAt(),
  updatedAt: updatedAt(),
});

export const doctorBookings = pgTable("doctor_bookings", {
  id: serial("id").primaryKey(),
  bookingCode: text("booking_code").notNull().unique(),
  doctorId: integer("doctor_id").notNull().references(() => doctors.id),
  patientName: text("patient_name").notNull(),
  patientPhone: text("patient_phone").notNull(),
  requestedDate: text("requested_date").notNull(),
  requestedTime: text("requested_time").notNull().default("first_come"),
  patientNote: text("patient_note"),
  whatsappOptIn: integer("whatsapp_opt_in").notNull().default(0),
  status: text("status", { enum: ["pending_payment", "receipt_submitted", "payment_confirmed", "payment_rejected", "confirmed", "cancelled"] }).notNull().default("pending_payment"),
  receiptFileName: text("receipt_file_name"),
  receiptMimeType: text("receipt_mime_type"),
  receiptObjectKey: text("receipt_object_key"),
  adminNote: text("admin_note"),
  reviewedAt: timestamp("reviewed_at", { withTimezone: true, mode: "string" }),
  createdAt: createdAt(),
  updatedAt: updatedAt(),
});

export const serviceRequests = pgTable("service_requests", {
  id: serial("id").primaryKey(),
  requestCode: text("request_code").notNull().unique(),
  serviceType: text("service_type", { enum: ["lab", "nurse", "physio"] }).notNull(),
  patientName: text("patient_name").notNull(),
  patientPhone: text("patient_phone").notNull(),
  requestedDate: text("requested_date").notNull(),
  requestedTime: text("requested_time").notNull(),
  city: text("city").notNull(),
  neighborhood: text("neighborhood"),
  locationNote: text("location_note"),
  serviceDetails: text("service_details"),
  status: text("status", { enum: ["pending", "accepted", "rejected", "cancelled"] }).notNull().default("pending"),
  createdAt: createdAt(),
  updatedAt: updatedAt(),
});

export const adminNotifications = pgTable("admin_notifications", {
  id: serial("id").primaryKey(),
  kind: text("kind", { enum: ["home_care_request"] }).notNull(),
  serviceRequestId: integer("service_request_id").references(() => serviceRequests.id),
  title: text("title").notNull(),
  body: text("body").notNull(),
  isRead: integer("is_read").notNull().default(0),
  createdAt: createdAt(),
  readAt: timestamp("read_at", { withTimezone: true, mode: "string" }),
});

export const adminSecurity = pgTable("admin_security", {
  key: text("key").primaryKey(),
  failedLoginAttempts: integer("failed_login_attempts").notNull().default(0),
  lockedUntil: timestamp("locked_until", { withTimezone: true, mode: "string" }),
  updatedAt: updatedAt(),
});

export type ProviderApplication = typeof providerApplications.$inferSelect;
export type ProviderDocument = typeof providerDocuments.$inferSelect;
export type Doctor = typeof doctors.$inferSelect;
export type DoctorBooking = typeof doctorBookings.$inferSelect;
export type ServiceRequest = typeof serviceRequests.$inferSelect;
export type AdminNotification = typeof adminNotifications.$inferSelect;
