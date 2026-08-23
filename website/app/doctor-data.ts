export const clinicDayValues = [
  "sunday",
  "monday",
  "tuesday",
  "wednesday",
  "thursday",
  "friday",
  "saturday",
] as const;

export type ClinicDay = typeof clinicDayValues[number];

export type DoctorInput = {
  fullName: string;
  specialty: string;
  clinicLocation: string;
  city: string;
  bookingCost: number;
  currency: string;
  clinicDays: string;
  clinicStartTime: string;
  clinicEndTime: string;
  slotDurationMinutes: number;
  availability: string;
  phone: string;
  supervisorName: string | null;
  supervisorPhone: string | null;
  paymentInstructions: string;
  paymentInstructionsVisible: number;
  status: "active" | "inactive";
  updatedAt: string;
};

function text(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function validTime(value: string): boolean {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(value);
}

export function parseDoctorInput(payload: unknown): { data?: DoctorInput; error?: string } {
  if (!payload || typeof payload !== "object") return { error: "بيانات الطبيب غير صحيحة." };
  const input = payload as Record<string, unknown>;
  const fullName = text(input.fullName, 160);
  const specialty = text(input.specialty, 160);
  const clinicLocation = text(input.clinicLocation, 240);
  const city = text(input.city, 100);
  const phone = text(input.phone, 40);
  const currency = text(input.currency, 12) || "SDG";
  const paymentInstructions = text(input.paymentInstructions, 2000);
  const clinicStartTime = text(input.clinicStartTime, 5);
  const clinicEndTime = text(input.clinicEndTime, 5);
  const bookingCost = Number(input.bookingCost);
  const slotDurationMinutes = Number(input.slotDurationMinutes);
  const rawDays = Array.isArray(input.clinicDays) ? input.clinicDays : [];
  const clinicDays = rawDays.filter((day): day is ClinicDay =>
    typeof day === "string" && clinicDayValues.includes(day as ClinicDay));

  if (fullName.length < 2 || specialty.length < 2 || clinicLocation.length < 2 || city.length < 2) {
    return { error: "أكمل اسم الطبيب والتخصص وموقع العيادة والمدينة." };
  }
  if (phone.length < 6) return { error: "أدخل رقم تواصل صحيح للطبيب." };
  if (!Number.isInteger(bookingCost) || bookingCost < 0) return { error: "تكلفة الحجز غير صحيحة." };
  if (!clinicDays.length) return { error: "اختر يوماً واحداً على الأقل لعمل العيادة." };
  if (!validTime(clinicStartTime) || !validTime(clinicEndTime) || clinicStartTime >= clinicEndTime) {
    return { error: "ساعات عمل العيادة غير صحيحة." };
  }
  if (![15, 30, 60].includes(slotDurationMinutes)) return { error: "مدة الموعد غير صحيحة." };
  if (!paymentInstructions) return { error: "اكتب تعليمات الدفع." };

  return {
    data: {
      fullName,
      specialty,
      clinicLocation,
      city,
      bookingCost,
      currency,
      clinicDays: JSON.stringify(clinicDays),
      clinicStartTime,
      clinicEndTime,
      slotDurationMinutes,
      availability: clinicDays.join(", "),
      phone,
      supervisorName: text(input.supervisorName, 160) || null,
      supervisorPhone: text(input.supervisorPhone, 40) || null,
      paymentInstructions,
      paymentInstructionsVisible: input.paymentInstructionsVisible ? 1 : 0,
      status: input.status === "inactive" ? "inactive" : "active",
      updatedAt: new Date().toISOString(),
    },
  };
}

export function isSudanesePhone(value: string): boolean {
  return /^((09\d{8})|(\+2499\d{8})|(002499\d{8}))$/.test(value.replace(/[\s()-]/g, ""));
}

export function makeCode(prefix: "BK" | "HM"): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}-${crypto.randomUUID().slice(0, 5).toUpperCase()}`;
}
