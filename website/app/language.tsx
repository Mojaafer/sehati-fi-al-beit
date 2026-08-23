"use client";

import { cloneElement, createContext, isValidElement, ReactElement, ReactNode, useContext, useEffect, useState } from "react";
import { usePathname } from "next/navigation";

export type Language = "ar" | "en";

const translations: Record<string, string> = {
  "منصة صحتك": "Sehatak Platform",
  "صحتك": "Sehatak",
  "الرعاية الصحية لحد باب بيتك": "Healthcare delivered to your doorstep",
  "الرعاية لحد باب بيتك": "Care delivered to your doorstep",
  "سلامتك أولويتنا — مقدمو خدمات يتم التحقق منهم": "Your safety is our priority — service providers are verified",
  "نبدأ من ود مدني": "Starting in Wad Madani",
  "التنقل الرئيسي": "Main navigation",
  "الخدمات": "Services",
  "كيف تعمل المنصة": "How the platform works",
  "لمقدمي الخدمات": "For service providers",
  "حجز موعد طبيب": "Book a doctor appointment",
  "عن المنصة": "About the platform",
  "الأسئلة الشائعة": "Frequently asked questions",
  "اطلب خدمة": "Request a service",
  "خدمة منزلية تبدأ من ود مدني": "Home care services starting in Wad Madani",
  "الرعاية الصحية...": "Healthcare...",
  "لحد باب بيتك": "delivered to your doorstep",
  "اطلب خدمات صحية منزلية من مقدمي خدمات موثّقين، وحدد الوقت والمكان المناسب ليك.": "Request home healthcare from verified providers and choose the time and location that suit you.",
  "اطلب خدمة الآن": "Request a service now",
  "انضم كمقدم خدمة": "Join as a service provider",
  "معاك خطوة بخطوة": "With you every step of the way",
  "دعم ومتابعة حتى تكتمل خدمتك": "Support and follow-up until your service is completed",
  "الوقت المناسب ليك": "A time that suits you",
  "اختار الموعد براحتك": "Choose your appointment at your convenience",
  "سلامتك أولويتنا": "Your safety is our priority",
  "بياناتك في أمان": "Your data is secure",
  "مزايا المنصة": "Platform benefits",
  "مقدمون موثّقون": "Verified providers",
  "اختيار الوقت المناسب": "Choose a convenient time",
  "متابعة الطلب": "Track your request",
  "أسعار واضحة قبل التأكيد": "Clear pricing before confirmation",
  "دعم ومتابعة": "Support and follow-up",
  "خدماتنا الحالية": "Our current services",
  "خدمة صحية،": "Healthcare",
  "بشكل أبسط": "made simpler",
  "اختر خدمة منزلية، أو افتح دليل الأطباء لحجز يوم حضور في العيادة.": "Choose a home service, or open the doctor directory to request a clinic visit day.",
  "سحب العينات من المنزل": "Home sample collection",
  "فني مختبر مؤهل يجيك للمنزل لسحب العينة وإرسالها للمعمل.": "A qualified lab technician comes to your home to collect the sample and send it to the laboratory.",
  "اطلب الخدمة": "Request the service",
  "التمريض المنزلي": "Home nursing",
  "تقييم تمريضي، قياسات حيوية، غيار جروح بسيط، وتعليم الأسرة حسب خطة الرعاية.": "Nursing assessments, vital-sign checks, basic wound dressing, and family guidance according to the care plan.",
  "اطلب ممرض/ة": "Request a nurse",
  "العلاج الطبيعي": "Physiotherapy",
  "تقييم حركة وتمارين منزلية آمنة للتأهيل، التوازن، والقدرة على الحركة.": "Mobility assessment and safe home exercises for rehabilitation, balance, and improved movement.",
  "احجز جلسة": "Book a session",
  "حجز موعد مع طبيب": "Book an appointment with a doctor",
  "اختار طبيباً معتمداً ويوم الحضور في العيادة حسب الجدول المتاح.": "Choose a verified doctor and an available clinic visit day.",
  "افتح دليل الأطباء": "Open the doctor directory",
  "تحقق واضح": "Clear verification",
  "قبل الخدمة": "before service",
  "الهوية": "Identity",
  "مطابقة البيانات": "Data matching",
  "المؤهل": "Qualification",
  "مراجعة الشهادة": "Certificate review",
  "الترخيص": "License",
  "التسجيل المهني": "Professional registration",
  "نخليها واضحة من البداية": "We make everything clear from the start",
  "أولويتنا": "our priority",
  "يتم التحقق من بيانات ووثائق مقدمي الخدمات قبل تفعيل حساباتهم. وأنت تعرف السعر، الموعد، ومقدم الخدمة قبل تأكيد طلبك.": "Service providers' details and documents are verified before their accounts are activated. You know the price, appointment, and provider before confirming your request.",
  "تحققنا من مقدم الخدمة": "We verified the service provider",
  "هوية، مؤهل، وترخيص مهني": "Identity, qualifications, and professional license",
  "متابعة بعد الطلب": "Follow-up after your request",
  "نحن معاك حتى تكتمل الخدمة": "We stay with you until the service is completed",
  "خصوصيتك محفوظة": "Your privacy is protected",
  "بياناتك لا تُشارك إلا عند الحاجة": "Your data is shared only when necessary",
  "كيف تعمل المنصة؟": "How does the platform work?",
  "من الطلب": "From your request",
  "لحد باب البيت": "to your doorstep",
  "أربع خطوات بسيطة، بدون لف ودوران.": "Four simple, straightforward steps.",
  "اختر الخدمة": "Choose a service",
  "قول لينا محتاج شنو": "Tell us what you need",
  "حدد موقعك وموعدك": "Choose your location and appointment",
  "اختار المكان والوقت المناسب": "Choose a convenient place and time",
  "أكد الطلب والدفع": "Confirm the request and payment",
  "راجع التفاصيل قبل التأكيد": "Review the details before confirming",
  "مقدم الخدمة يجيك": "The service provider comes to you",
  "ونتابع معاك لحد النهاية": "And we follow up with you to the end",
  "للمهنيين الصحيين": "For healthcare professionals",
  "اشتغل معانا،": "Work with us",
  "بوقتك وخبرتك": "on your schedule, using your expertise",
  "أنت تحدد الأيام والساعات المتاح فيها. نقدم ليك طريقة مرنة لتقديم خدمات صحية إضافية لأهل مدينتك.": "You choose your available days and hours. We offer a flexible way to provide additional healthcare services to people in your city.",
  "أطباء للحجز بالعيادة": "Doctors available for clinic bookings",
  "تمريض": "Nursing",
  "فنيو مختبر": "Lab technicians",
  "علاج طبيعي": "Physiotherapy",
  "إرسال طلب الانضمام": "Submit an application to join",
  "طلب انضمام مقدم خدمة": "Service provider application",
  "ارفع مستنداتك": "Upload your documents",
  "الهوية، المؤهل، والترخيص": "Identity, qualifications, and license",
  "نراجع بياناتك": "We review your information",
  "فريق التحقق يتأكد من المعلومات": "The verification team checks your information",
  "ابدأ استقبال الطلبات": "Start receiving requests",
  "بعد اكتمال التحقق فقط": "Only after verification is complete",
  "مافي مقدم خدمة بيظهر قبل التحقق": "No service provider is listed before verification",
  "الثقة جزء من طريقة عمل المنصة": "Trust is built into how the platform works",
  "أسئلة سريعة": "Quick questions",
  "قبل ما": "Before you",
  "تطلب": "request a service",
  "إجابات واضحة عن التغطية، التحقق، المواعيد، الدفع، وخصوصية بياناتك.": "Clear answers about coverage, verification, appointments, payment, and data privacy.",
  "هل الخدمة متوفرة خارج ود مدني؟": "Is the service available outside Wad Madani?",
  "الخدمة متوفرة حالياً داخل ود مدني خلال الفترة التجريبية، والتوسع لبقية المدن سيتم بصورة تدريجية.": "The service is currently available within Wad Madani during the pilot period, with gradual expansion to other cities.",
  "كيف أتأكد من مقدم الخدمة؟": "How can I verify the service provider?",
  "تراجع منصة صحتك الهوية والمؤهل والترخيص المهني قبل السماح لمقدم الخدمة باستقبال الطلبات.": "Sehatak reviews the provider's identity, qualifications, and professional license before allowing them to receive requests.",
  "هل أقدر أختار الوقت بنفسي؟": "Can I choose the time myself?",
  "في الخدمات المنزلية تختار الفترة المناسبة عند إرسال الطلب. أما الأطباء فتختار يوم الحضور من جدول العيادة، والدخول بأسبقية الحضور.": "For home services, choose a suitable time window. For doctors, choose a clinic day; patients are seen first-come, first-served.",
  "ما هي طرق الدفع المتاحة؟": "What payment methods are available?",
  "الدفع يتم إلى الحسابات البنكية الخاصة بمنصة صحتك عبر بنكك أو التطبيقات البنكية الأخرى، ثم ترفع صورة إشعار التحويل عند طلبها.": "Pay into Sehatak's bank accounts through Bankak or another banking app, then upload the transfer receipt when requested.",
  "هل يتأكد الموعد مباشرة بعد إرسال الطلب؟": "Is the appointment confirmed immediately after submitting the request?",
  "لا. تتم مراجعة الطلب وإشعار التحويل أولاً، ثم يظهر التأكيد بعد قبول الطلب واستكمال الخطوات.": "No. The request and transfer receipt are reviewed first, then confirmation appears after approval.",
  "من يرى بياناتي وإشعار التحويل؟": "Who can see my data and transfer receipt?",
  "تُستخدم البيانات لمعالجة طلبك ومراجعة الدفع، ولا يصل إليها إلا الأشخاص المصرح لهم بإدارة الطلب.": "The data is used to process your request and review payment, and is accessible only to authorized staff.",
  "هذه المنصة ليست بديلاً لخدمات الطوارئ.": "This platform is not a substitute for emergency services.",
  "في حالة النزيف الشديد، صعوبة التنفس، فقدان الوعي، أو ألم الصدر الشديد، توجه لأقرب قسم طوارئ أو اتصل بخدمات الطوارئ المتاحة في منطقتك.": "For severe bleeding, difficulty breathing, loss of consciousness, or severe chest pain, go to the nearest emergency department or contact local emergency services.",
  "روابط سريعة": "Quick links",
  "دليل الأطباء": "Doctor directory",
  "نطاق التجربة": "Pilot coverage",
  "ود مدني، السودان": "Wad Madani, Sudan",
  "نسخة MVP تجريبية": "Pilot MVP version",
  "دخول الإدارة": "Admin login",
  "صُنعت لأهل السودان، من ود مدني": "Made for the people of Sudan, from Wad Madani",
  "الخدمة": "Service",
  "التفاصيل": "Details",
  "الموقع": "Location",
  "الموعد": "Appointment",
  "المريض": "Patient",
  "التأكيد": "Confirmation",
  "طلب خدمة منزلية": "Home service request",
  "ملخص الطلب التجريبي": "Request summary",
  "إغلاق": "Close",
  "تم استلام طلبك": "Your request has been received",
  "طلبك في طريقه للمراجعة": "Your request is being reviewed",
  "سيتواصل معك فريق صحتك لتأكيد تفاصيل الخدمة وتعيين مقدم الخدمة.": "The Sehatak team will contact you to confirm the service details and assign a provider.",
  "رقم الطلب": "Request number",
  "التكلفة": "Cost",
  "تُؤكد بعد مراجعة التفاصيل": "Confirmed after review",
  "العودة للرئيسية": "Return to home",
  "نرتبها معاك خطوة بخطوة": "We'll arrange it with you step by step",
  "شنو الخدمة المحتاجها؟": "Which service do you need?",
  "شنو الفحص المطلوب؟": "Which test do you need?",
  "شنو نوع التمريض المحتاجه؟": "What type of nursing care do you need?",
  "شنو هدف الجلسة؟": "What is the goal of the session?",
  "شنو سبب الزيارة؟": "What is the reason for the visit?",
  "نوع التحليل": "Test type",
  "تحليل دم شامل": "Complete blood count",
  "سكر صائم": "Fasting blood sugar",
  "وظائف كلى": "Kidney function tests",
  "وظائف كبد": "Liver function tests",
  "دهون الدم": "Lipid profile",
  "تحليل بول": "Urinalysis",
  "هل عندك طلب فحص من طبيب؟": "Do you have a test request from a doctor?",
  "اختر": "Select",
  "يوجد طلب فحص من طبيب": "I have a test request from a doctor",
  "لا يوجد طلب فحص من طبيب": "I do not have a test request from a doctor",
  "ملاحظات إضافية": "Additional notes",
  "اكتب أي تفاصيل تساعدنا نخدمك أفضل": "Enter any details that will help us serve you better",
  "الخدمة المطلوبة": "Required service",
  "تقييم تمريضي وقياس العلامات الحيوية": "Nursing assessment and vital-sign measurement",
  "غيار جرح بسيط حسب وصفة أو خطة رعاية": "Basic wound dressing according to a prescription or care plan",
  "الوقاية من قرح الضغط وتغيير الوضعية": "Pressure-ulcer prevention and repositioning",
  "متابعة بعد الخروج وتعليم الأسرة": "Post-discharge follow-up and family education",
  "هل توجد وصفة أو خطة رعاية؟": "Is there a prescription or care plan?",
  "نعم، توجد وصفة أو خطة رعاية": "Yes, there is a prescription or care plan",
  "لا، أحتاج تقييم الممرض/ة أولاً": "No, I need a nurse's assessment first",
  "لا تشمل الخدمة الطوارئ، تغيير جرعات الأدوية، أو أي إجراء غير موصوف.": "The service does not cover emergencies, medication dosage changes, or unprescribed procedures.",
  "الهدف من الجلسة": "Session goal",
  "تقييم الحركة ووضع خطة تمارين": "Mobility assessment and exercise planning",
  "تأهيل بعد إصابة أو عملية": "Rehabilitation after an injury or operation",
  "تمارين التوازن وتقليل خطر السقوط": "Balance exercises and fall-risk reduction",
  "تحسين المشي والانتقال من السرير": "Improving walking and transfers from bed",
  "هل لديك تشخيص أو تقرير طبي؟": "Do you have a diagnosis or medical report?",
  "نعم، يوجد تشخيص أو تقرير طبي": "Yes, I have a diagnosis or medical report",
  "لا، أحتاج تقييم أخصائي أولاً": "No, I need a specialist assessment first",
  "يبدأ العلاج بتقييم أخصائي، ولا يستخدم للطوارئ أو بعد إصابة حادة غير مُقيّمة.": "Treatment begins with a specialist assessment and is not for emergencies or unevaluated acute injuries.",
  "وين محتاج الخدمة؟": "Where do you need the service?",
  "المدينة": "City",
  "ود مدني": "Wad Madani",
  "الخرطوم": "Khartoum",
  "سنار": "Sennar",
  "الحي": "Neighborhood",
  "مثال: حي الدرجة": "Example: Al-Daraja neighborhood",
  "وصف الموقع": "Location description",
  "اسم الشارع أو رقم المنزل": "Street name or house number",
  "اختار الوقت المناسب": "Choose a suitable time",
  "غداً": "Tomorrow",
  "أقرب موعد": "Earliest appointment",
  "بعد غد": "The day after tomorrow",
  "المواعيد المتاحة": "Available appointments",
  "بيانات الشخص المستفيد": "Beneficiary details",
  "الاسم": "Name",
  "الاسم الكامل": "Full name",
  "رقم الهاتف": "Phone number",
  "العمر": "Age",
  "الجنس": "Gender",
  "ذكر": "Male",
  "أنثى": "Female",
  "ملاحظات طبية": "Medical notes",
  "اختياري": "Optional",
  "أي معلومات مهمة للفريق": "Any important information for the team",
  "بياناتك الطبية لا يتم مشاركتها إلا مع مقدم الخدمة المكلف بطلبك.": "Your medical data is shared only with the provider assigned to your request.",
  "راجع طلبك قبل الإرسال": "Review your request before submitting",
  "الموعد المطلوب": "Requested appointment",
  "تُحدد بعد المراجعة": "Determined after review",
  "إرسال الطلب لا يعني تأكيد الموعد أو السعر. سيتواصل الفريق معك بعد مراجعته.": "Submitting does not confirm the appointment or price. The team will contact you after review.",
  "رجوع": "Back",
  "جاري الإرسال...": "Submitting...",
  "إرسال الطلب": "Submit request",
  "التالي": "Next",
  "تم إرسال طلب مقدم الخدمة": "Service provider application submitted",
  "تم إرسال الطلب بنجاح": "The application was submitted successfully",
  "شكراً لانضمامك لينا": "Thank you for joining us",
  "حنراجع بياناتك ومستنداتك، ونتواصل معاك عبر الهاتف لتأكيد الخطوات القادمة.": "We'll review your information and documents and contact you by phone with the next steps.",
  "لا يستطيع مقدم الخدمة استقبال طلبات قبل إكمال التحقق من الهوية والمؤهل والترخيص المهني.": "A provider cannot receive requests until identity, qualifications, and licensing are verified.",
  "انضمام مقدم خدمة": "Service provider registration",
  "طبيب/ة": "Doctor",
  "فني/ة مختبرات": "Lab technician",
  "أخصائي/ة علاج طبيعي": "Physiotherapist",
  "ممرض/ة": "Nurse",
  "بوابة مقدمي الخدمات": "Service provider portal",
  "سجّل مهنتك، وخلي خبرتك أقرب للناس": "Register your profession and bring your expertise closer to people",
  "أنت تحدد الأيام والساعات المتاح فيها، ونحن نراجع بياناتك قبل تفعيل استقبال الطلبات.": "You choose your availability, and we review your information before enabling requests.",
  "اختار مهنتك": "Choose your profession",
  "بياناتك الأساسية": "Your basic information",
  "كما في الهوية": "As shown on your ID",
  "مثال: طب أطفال": "Example: Pediatrics",
  "سنوات الخبرة": "Years of experience",
  "أقل من سنة": "Less than one year",
  "1 – 3 سنوات": "1–3 years",
  "4 – 7 سنوات": "4–7 years",
  "أكثر من 7 سنوات": "More than 7 years",
  "مكان العمل الحالي": "Current workplace",
  "اسم المستشفى أو المركز": "Hospital or center name",
  "الأوقات المتاحة": "Availability",
  "مثال: السبت والثلاثاء، 5 – 10 مساءً": "Example: Saturday and Tuesday, 5–10 PM",
  "مستندات التحقق": "Verification documents",
  "الهوية والمؤهل مطلوبان. الحد الأقصى 5 ميغابايت لكل ملف، PDF أو صورة.": "Identity and qualification documents are required. Each file must be a PDF or image no larger than 5 MB.",
  "السيرة الذاتية": "CV",
  "PDF أو صورة": "PDF or image",
  "الشهادة *": "Qualification certificate *",
  "المؤهل الأكاديمي": "Academic qualification",
  "الترخيص المهني": "Professional license",
  "إن وجد": "If available",
  "بطاقة الهوية *": "Identity card *",
  "صورة واضحة": "Clear image",
  "نستخدم بياناتك للتحقق والتواصل معك فقط، ولا نعرض معلوماتك الحساسة للمرضى.": "We use your data only for verification and communication and do not show sensitive information to patients.",
  "إلغاء": "Cancel",
  "إرسال طلب الانضمام ←": "Submit application →",
  "فتح مساعد سند": "Open Sanad assistant",
  "اسأل سَند": "Ask Sanad",
  "مساعدك من صحتك": "Your Sehatak assistant",
  "محادثة سند": "Sanad conversation",
  "سَند": "Sanad",
  "مساعد صحتك": "Sehatak assistant",
  "معلومات عن خدمات المنصة والحجز فقط، وليست تشخيصاً أو بديلاً عن الطبيب.": "Information about services and bookings only; this is not a diagnosis or a substitute for a doctor.",
  "مرحباً، أنا سَند. قول لي محتاج خدمة شنو وأنا أوجّهك للمكان المناسب داخل صحتك.": "Hello, I'm Sanad. Tell me what service you need and I'll direct you to the right place.",
  "دي ممكن تكون حالة طارئة. توجّه فوراً لأقرب قسم طوارئ أو اتصل بخدمة الطوارئ المتاحة في منطقتك.": "This may be an emergency. Go immediately to the nearest emergency department or contact local emergency services.",
  "الخدمة متوفرة حالياً داخل ود مدني في الفترة التجريبية، والتوسع للمدن والولايات الأخرى حيكون بصورة تدريجية.": "The service is currently available in Wad Madani during the pilot, with gradual expansion to other cities and states.",
  "بياناتك وإشعار التحويل تُستخدم لمعالجة الطلب ومراجعة الدفع، ولا يصل إليها إلا فريق الإدارة المصرح له.": "Your data and transfer receipt are used to process the request and are accessible only to authorized administrators.",
  "الدفع يكون إلى حسابات منصة صحتك عبر بنكك أو التطبيقات البنكية الأخرى، ثم ترفع صورة إشعار التحويل عند طلبها.": "Pay into Sehatak's accounts through Bankak or another banking app, then upload the transfer receipt when requested.",
  "حجز الطبيب يكون للحضور في العيادة، وليس زيارة منزلية. اختر الطبيب ويوم الحضور من الدليل، والدخول بأسبقية الحضور.": "Doctor bookings are for clinic visits, not home visits. Choose the doctor and day; patients are seen first-come, first-served.",
  "خدمة سحب العينات تتم في المنزل بواسطة فني مختبر، ثم تُرسل العينة للمعمل.": "A lab technician collects the sample at home and sends it to the laboratory.",
  "اطلب سحب العينات": "Request sample collection",
  "أقدر أفتح ليك طلب العلاج الطبيعي المنزلي لتقييم الحركة ووضع خطة مناسبة.": "I can open a home physiotherapy request for an assessment and suitable plan.",
  "اطلب العلاج الطبيعي": "Request physiotherapy",
  "أقدر أفتح ليك طلب التمريض المنزلي. الخدمة لا تشمل الطوارئ أو تغيير جرعات الأدوية.": "I can open a home nursing request. The service does not cover emergencies or dosage changes.",
  "اطلب التمريض المنزلي": "Request home nursing",
  "أنا سَند، أساعدك تختار بين سحب العينات، التمريض المنزلي، العلاج الطبيعي، أو حجز موعد طبيب في العيادة. محتاج أي واحدة؟": "I'm Sanad. I can help with sample collection, home nursing, physiotherapy, or a clinic appointment. Which do you need?",
  "حجز طبيب": "Book a doctor",
  "سحب عينة": "Sample collection",
  "تمريض منزلي": "Home nursing",
  "أريد حجز موعد مع طبيب": "I want to book an appointment with a doctor",
  "أحتاج سحب عينة": "I need sample collection",
  "أحتاج تمريض منزلي": "I need home nursing",
  "اكتب الخدمة المحتاجها...": "Type the service you need...",
  "إرسال": "Send",
  "اختار الطبيب المناسب ليك": "Choose the right doctor for you",
  "اختر يوم الحضور من جدول العيادة. الدخول بأسبقية الحضور، والطلب لا يتأكد إلا بعد مراجعة إشعار التحويل.": "Choose a clinic day from the doctor's schedule. Patients are seen first-come, first-served, and requests are confirmed after receipt review.",
  "جاري تحميل الأطباء...": "Loading doctors...",
  "متاح للحجز": "Available for booking",
  "جدول العيادة:": "Clinic schedule:",
  "لم يحدد بعد": "Not specified yet",
  "تكلفة الحجز": "Booking fee",
  "ابدأ طلب الحجز": "Start booking request",
  "لم يحدد الطبيب مواعيده بعد": "The doctor has not set a schedule yet",
  "سيظهر الأطباء المعتمدون هنا قريباً.": "Verified doctors will appear here soon.",
  "حجز موعد في العيادة": "Book a clinic appointment",
  "اختر يوم الحضور فقط. لا يوجد وقت محجوز لكل مريض؛ الدخول يكون بأسبقية الحضور داخل ساعات العيادة.": "Choose only the visit day. Patients are seen first-come, first-served during clinic hours.",
  "يوم الحضور": "Visit day",
  "ملاحظة اختيارية": "Optional note",
  "أوافق على استلام تحديثات الحجز عبر واتساب على الرقم المدخل.": "I agree to receive booking updates via WhatsApp at this number.",
  "إرسال طلب الحضور": "Submit visit request",
  "تم إنشاء الطلب. ارفع صورة إشعار التحويل لإرساله للمراجعة.": "Request created. Upload the transfer receipt to submit it for review.",
  "تعليمات الدفع ستظهر بعد تأكيدها من الإدارة.": "Payment instructions will appear after admin confirmation.",
  "احتفظ بالرقم.": "Keep this number.",
  "ارفع صورة إشعار التحويل": "Upload the transfer receipt",
  "PNG أو JPG أو WEBP — بحد أقصى 5 ميغابايت": "PNG, JPG, or WEBP — maximum 5 MB",
  "جاري الرفع...": "Uploading...",
  "إرسال إشعار التحويل": "Submit transfer receipt",
  "تم رفع إشعار التحويل، وسيراجعه فريق صحتك.": "The transfer receipt was uploaded and will be reviewed by the Sehatak team.",
  "تم إلغاء تحديثات واتساب لهذا الطلب.": "WhatsApp updates have been disabled for this request.",
  "إلغاء تحديثات واتساب": "Disable WhatsApp updates",
  "الأحد": "Sunday", "الاثنين": "Monday", "الثلاثاء": "Tuesday", "الأربعاء": "Wednesday",
  "الخميس": "Thursday", "الجمعة": "Friday", "السبت": "Saturday", "ص": "AM", "م": "PM",
  "دخول آمن": "Secure login",
  "لوحة إدارة مقدمي الخدمة": "Service Provider Admin Dashboard",
  "أدخل اسم المستخدم وكلمة المرور المخصصة للإدارة.": "Enter the administrator username and password.",
  "الجلسة مشفّرة وتنتهي تلقائياً بعد 8 ساعات.": "The session is encrypted and expires automatically after 8 hours.",
  "اسم المستخدم": "Username", "كلمة المرور": "Password", "جاري التحقق...": "Verifying...",
  "دخول لوحة الإدارة": "Log in to the admin dashboard", "مسح جلسة هذا الجهاز": "Clear this device's session",
  "لوحة الإدارة": "Admin dashboard", "الأطباء": "Doctors", "الحجوزات والمدفوعات": "Bookings and payments",
  "مقدمو الخدمة": "Service providers", "الخدمات المنزلية": "Home services", "القائمة": "Menu",
  "إشعارات الإدارة": "Admin notifications", "تسجيل الخروج": "Log out", "تحديد الكل كمقروء": "Mark all as read",
  "لا توجد إشعارات حالياً.": "There are currently no notifications.", "مسؤول النظام": "System administrator",
  "طلب خدمة منزلية جديد": "New home service request", "سحب عينات": "Sample collection",
  "إدارة الدليل الطبي": "Doctor directory management", "الأطباء والعيادات": "Doctors and clinics",
  "أضف الأطباء وحدد أيام وساعات العيادة وتعليمات الدفع.": "Add doctors and configure clinic days, hours, and payment instructions.",
  "إضافة طبيب": "Add doctor", "تحديث": "Refresh", "ظاهر للزوار": "Visible to visitors", "مخفي": "Hidden",
  "إخفاء": "Hide", "إظهار": "Show", "حذف": "Delete", "لا يوجد أطباء مضافون بعد.": "No doctors have been added yet.",
  "تعديل سجل الطبيب": "Edit doctor record", "إضافة سجل جديد": "Add a new record", "بيانات الطبيب": "Doctor details",
  "اسم الطبيب": "Doctor name", "التخصص": "Specialty", "موقع العيادة": "Clinic location", "رقم التواصل": "Contact number",
  "العملة": "Currency", "اسم مشرف الجدول": "Schedule supervisor name", "هاتف المشرف": "Supervisor phone number",
  "أيام عمل العيادة": "Clinic working days", "من": "From", "إلى": "To", "مدة الموعد": "Appointment duration",
  "15 دقيقة": "15 minutes", "30 دقيقة": "30 minutes", "60 دقيقة": "60 minutes", "تعليمات الدفع": "Payment instructions",
  "إظهار تعليمات الدفع": "Show payment instructions", "ظاهر في الدليل": "Visible in the directory", "جاري الحفظ...": "Saving...",
  "حفظ التعديلات": "Save changes", "حفظ الطبيب": "Save doctor", "مراجعة الحجوزات": "Booking review",
  "تم تحديث بيانات الطبيب.": "The doctor's details have been updated.",
  "تمت إضافة الطبيب إلى الدليل.": "The doctor has been added to the directory.",
  "تم حذف الطبيب.": "The doctor has been deleted.",
  "تم إظهار الطبيب للزوار.": "The doctor is now visible to visitors.",
  "تم إخفاء الطبيب من الدليل.": "The doctor has been hidden from the directory.",
  "طلبات حجز الأطباء": "Doctor booking requests", "الحضور بأسبقية الوصول، ولا يعتمد الطلب قبل مراجعة التحويل.": "Patients are seen in order of arrival; requests are approved after transfer review.",
  "جاري تحميل الحجوزات...": "Loading bookings...", "الدخول بأسبقية الحضور": "First-come, first-served",
  "ملاحظة طالب الخدمة:": "Requester's note:", "فتح صورة إشعار التحويل": "Open transfer receipt",
  "واتساب طالب الخدمة": "Message requester on WhatsApp", "واتساب المشرف": "Message supervisor on WhatsApp",
  "لم يوافق طالب الخدمة على تحديثات واتساب.": "The requester did not consent to WhatsApp updates.",
  "اعتماد التحويل": "Approve transfer", "رفض الإشعار": "Reject receipt", "تأكيد الموعد": "Confirm appointment",
  "حذف نهائي": "Delete permanently", "لا توجد طلبات حجز حتى الآن.": "There are no booking requests yet.",
  "تم تحديث حالة الحجز.": "The booking status has been updated.", "تم حذف طلب الحجز.": "The booking request has been deleted.",
  "هل تريد حذف طلب الحجز وصورة الإشعار نهائياً؟": "Do you want to permanently delete the booking request and receipt image?",
  "يرجى رفع صورة واضحة لإشعار التحويل.": "Please upload a clear image of the transfer receipt.",
  "بانتظار التحويل": "Awaiting transfer", "الإشعار للمراجعة": "Receipt under review", "تم اعتماد الدفع": "Payment approved",
  "الإشعار مرفوض": "Receipt rejected", "الموعد مؤكد": "Appointment confirmed", "ملغى": "Cancelled",
  "جديد": "New", "قيد المراجعة": "Under review", "يحتاج مستندات": "Additional documents required",
  "تم التحقق": "Verified", "مرفوض": "Rejected", "قيد الفحص": "Pending review", "مقبول": "Accepted",
  "العودة للموقع": "Return to website", "مركز التحقق": "Verification center", "طلبات مقدمي الخدمة": "Service provider applications",
  "تحديث القائمة": "Refresh list", "جاري تحميل الطلبات...": "Loading requests...", "لا توجد طلبات حتى الآن.": "There are no requests yet.",
  "الهاتف": "Phone", "الخبرة": "Experience", "مكان العمل": "Workplace", "التوفر": "Availability",
  "غير محدد": "Not specified", "غير محددة": "Not specified", "المستندات": "Documents", "رفض": "Reject",
  "لم يتم رفع مستندات.": "No documents have been uploaded.", "ملاحظات الإدارة": "Admin notes",
  "اكتب ملاحظة لفريق المراجعة": "Write a note for the review team", "حفظ الملاحظة": "Save note",
  "اعتماد الطلب": "Approve application", "طلب مستندات": "Request documents", "رفض الطلب": "Reject application",
  "حذف الطلب نهائياً": "Delete application permanently", "اختر طلباً لعرض التفاصيل.": "Select an application to view its details.",
  "هل تريد حذف طلب مقدم الخدمة ومستنداته نهائياً؟": "Do you want to permanently delete the provider application and its documents?",
  "سحب العينات": "Sample collection", "طلبات الخدمات المنزلية": "Home service requests",
  "طلبات التمريض وسحب العينات والعلاج الطبيعي المحفوظة من الموقع.": "Nursing, sample collection, and physiotherapy requests submitted through the website.",
  "وصف الموقع:": "Location description:", "تفاصيل الخدمة:": "Service details:", "اتصال بطالب الخدمة": "Call requester",
  "لا توجد طلبات خدمات منزلية حتى الآن.": "There are no home service requests yet.",
  "تم حذف طلب الخدمة.": "The service request has been deleted.", "هل تريد حذف طلب الخدمة نهائياً؟": "Do you want to permanently delete this service request?",
  "تعذر إرسال الطلب.": "The request could not be submitted.", "تعذر تحميل الطلبات.": "The requests could not be loaded.",
  "تعذر تحميل الأطباء.": "The doctors could not be loaded.", "تعذر تحميل الحجوزات.": "The bookings could not be loaded.",
  "تعذر حفظ الطبيب.": "The doctor could not be saved.", "تعذر حذف الطبيب.": "The doctor could not be deleted.",
  "تعذر تحديث الحجز.": "The booking could not be updated.", "تعذر حذف الحجز.": "The booking could not be deleted.",
  "تعذر حفظ التحديث.": "The update could not be saved.", "تعذر حذف الطلب.": "The request could not be deleted.",
  "تعذر تسجيل الدخول.": "Unable to log in.", "بيانات الدخول غير صحيحة.": "The login credentials are incorrect.",
  "تم إيقاف الدخول مؤقتاً بعد محاولات متكررة. حاول بعد 15 دقيقة.": "Login has been temporarily suspended after repeated attempts. Try again in 15 minutes.",
  "اختر تفاصيل الخدمة المطلوبة.": "Choose the required service details.",
  "أكمل المدينة والحي ووصف الموقع.": "Complete the city, neighborhood, and location description.",
  "اختر الوقت المناسب.": "Choose a suitable time.",
  "أدخل اسم المستفيد ورقم هاتف سوداني صحيح مثل 09XXXXXXXX.": "Enter the beneficiary's name and a valid Sudanese phone number such as 09XXXXXXXX."
};

const orderedTranslations = Object.entries(translations).sort(([left], [right]) => right.length - left.length);

type LanguageContextValue = {
  language: Language;
  direction: "rtl" | "ltr";
  locale: "ar-SD" | "en-US";
  toggleLanguage(): void;
  t(value: string): string;
};

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function translate(value: string, language: Language): string {
  if (language === "ar" || !/[\u0600-\u06ff]/.test(value)) return value;
  if (translations[value]) return translations[value];
  return orderedTranslations.reduce((result, [arabic, english]) => result.replaceAll(arabic, english), value);
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>("ar");

  useEffect(() => {
    const saved = window.localStorage.getItem("sehatak-language");
    if (saved === "en") setLanguage("en");
  }, []);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = language === "ar" ? "rtl" : "ltr";
    window.localStorage.setItem("sehatak-language", language);
  }, [language]);

  return (
    <LanguageContext.Provider value={{
      language,
      direction: language === "ar" ? "rtl" : "ltr",
      locale: language === "ar" ? "ar-SD" : "en-US",
      toggleLanguage: () => setLanguage((current) => current === "ar" ? "en" : "ar"),
      t: (value) => translate(value, language),
    }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) throw new Error("useLanguage must be used inside LanguageProvider");
  return context;
}

function localizeNode(node: ReactNode, language: Language): ReactNode {
  if (typeof node === "string") return translate(node, language);
  if (Array.isArray(node)) return node.map((child) => localizeNode(child, language));
  if (!isValidElement(node)) return node;

  const element = node as ReactElement<Record<string, unknown>>;
  const props = element.props;
  const nextProps: Record<string, unknown> = {};
  for (const name of ["aria-label", "alt", "placeholder", "title"]) {
    if (typeof props[name] === "string") nextProps[name] = translate(props[name], language);
  }
  if (typeof element.type === "string" && props.dir) nextProps.dir = language === "ar" ? "rtl" : "ltr";
  if (element.type === "option" && props.value === undefined && typeof props.children === "string") {
    nextProps.value = props.children;
  }
  if (props.children !== undefined) nextProps.children = localizeNode(props.children as ReactNode, language);
  return cloneElement(element, nextProps);
}

export function Localized({ children }: { children: ReactNode }) {
  const { language } = useLanguage();
  return localizeNode(children, language);
}

export function LanguageSwitch({ placement = "floating" }: { placement?: "floating" | "header" }) {
  const pathname = usePathname();
  const { language, toggleLanguage } = useLanguage();
  const target = language === "ar" ? "English" : "العربية";
  if (placement === "floating" && pathname === "/") return null;
  return (
    <button className={`language-switch language-switch-${placement}`} type="button" onClick={toggleLanguage} aria-label={language === "ar" ? "Switch to English" : "التبديل إلى العربية"}>
      <span aria-hidden="true">A / ع</span>{target}
    </button>
  );
}
