export const categories = [
  "CAMERAS",
  "AUDIO",
  "LIGHTING",
  "COMPUTING",
  "ACCESSORIES",
] as const;
export type Category = (typeof categories)[number];
export const categoryNames: Record<Category, string> = {
  CAMERAS: "Cameras",
  AUDIO: "Audio",
  LIGHTING: "Lighting",
  COMPUTING: "Computing",
  ACCESSORIES: "Accessories",
};
export type Equipment = {
  id: string;
  name: string;
  model: string;
  category: Category;
  location: string;
  description: string;
  totalQuantity: number;
  availableQuantity: number;
  createdAt: string;
};
export type Borrower = {
  id: string;
  name: string;
  team: string;
  activeLoans: number;
  createdAt: string;
};
export type Loan = {
  id: string;
  equipmentId: string;
  equipmentName: string;
  equipmentModel: string;
  category: Category;
  borrowerId: string;
  borrowerName: string;
  borrowerTeam: string;
  quantity: number;
  dueDate: string;
  notes: string;
  checkedOutAt: string;
  returnedAt: string | null;
  status: "ACTIVE" | "OVERDUE" | "RETURNED";
};
export type Page<T> = {
  items: T[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
};
export type Dashboard = {
  equipmentTypes: number;
  totalUnits: number;
  availableUnits: number;
  activeLoans: number;
  overdueLoans: number;
  borrowers: number;
  returnedLoans: number;
  recentLoans: Loan[];
  featuredEquipment: Equipment[];
  categories: { category: Category; types: number; total: number; available: number }[];
};
export type Session = {
  csrfToken: string;
  mode: "preview" | "mysql";
  isDemo: boolean;
  demoEnabled: boolean;
};
export type View = "overview" | "equipment" | "loans" | "people";
export type EquipmentInput = Omit<Equipment, "id" | "availableQuantity" | "createdAt">;
export type Filters = { search: string; category: string; status: string; page: number };
