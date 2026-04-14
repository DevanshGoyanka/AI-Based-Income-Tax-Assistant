import Card from '../components/ui/Card';

const PL_ROWS = [
  { label: 'Filing Fees — ITR-1', amount: 287500, type: 'income' },
  { label: 'Filing Fees — ITR-2', amount: 456000, type: 'income' },
  { label: 'Filing Fees — ITR-3/4', amount: 198000, type: 'income' },
  { label: 'Consultation Fees', amount: 84000, type: 'income' },
  { label: 'Notice Handling Fees', amount: 52000, type: 'income' },
];

const EXPENSE_ROWS = [
  { label: 'Office Rent', amount: 45000, type: 'expense' },
  { label: 'Staff Salaries', amount: 180000, type: 'expense' },
  { label: 'Software Subscriptions', amount: 12500, type: 'expense' },
  { label: 'Miscellaneous', amount: 8200, type: 'expense' },
];

const TRANSACTIONS = [
  { date: 'Apr 11', desc: 'Filing Fees — Rajesh Kumar', amount: 3500, type: 'credit' },
  { date: 'Apr 10', desc: 'Rent Payment', amount: -45000, type: 'debit' },
  { date: 'Apr 9', desc: 'Filing Fees — Priya Sharma', amount: 2500, type: 'credit' },
];

export default function Accounting() {
  const totalIncome = PL_ROWS.reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = EXPENSE_ROWS.reduce((sum, r) => sum + r.amount, 0);
  const netProfit = totalIncome - totalExpense;

  return (
    <div className="grid grid-cols-[1fr_380px] gap-4">
      <Card>
        <div className="text-[14px] font-semibold mb-4">Monthly P&L — April 2026</div>
        <div className="space-y-2 mb-4">
          <div className="text-[11px] font-bold uppercase tracking-wider text-text-muted mb-2">Income</div>
          {PL_ROWS.map(row => (
            <div key={row.label} className="flex justify-between py-1.5 border-b border-border">
              <span className="text-[13px] text-text-secondary">{row.label}</span>
              <span className="text-[13px] font-mono font-medium text-success">+₹{row.amount.toLocaleString('en-IN')}</span>
            </div>
          ))}
          <div className="flex justify-between py-2 font-bold">
            <span className="text-[13px]">Total Income</span>
            <span className="font-serif text-[17px] text-success">₹{totalIncome.toLocaleString('en-IN')}</span>
          </div>
        </div>
        <div className="space-y-2 mb-4">
          <div className="text-[11px] font-bold uppercase tracking-wider text-text-muted mb-2">Expenses</div>
          {EXPENSE_ROWS.map(row => (
            <div key={row.label} className="flex justify-between py-1.5 border-b border-border">
              <span className="text-[13px] text-text-secondary">{row.label}</span>
              <span className="text-[13px] font-mono font-medium text-danger">−₹{row.amount.toLocaleString('en-IN')}</span>
            </div>
          ))}
          <div className="flex justify-between py-2 font-bold">
            <span className="text-[13px]">Total Expenses</span>
            <span className="font-serif text-[17px] text-danger">₹{totalExpense.toLocaleString('en-IN')}</span>
          </div>
        </div>
        <div className="flex justify-between py-3 border-t-2 border-border-strong">
          <span className="text-[15px] font-bold">Net Profit</span>
          <span className="font-serif text-[24px] font-semibold text-accent-blue">₹{netProfit.toLocaleString('en-IN')}</span>
        </div>
      </Card>

      <div className="space-y-4">
        <Card>
          <div className="text-[14px] font-semibold mb-4">Bank Register</div>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-bg rounded-lg">
              <div>
                <div className="text-[13px] font-semibold">HDFC Bank — Current</div>
                <div className="text-[11px] text-text-muted">XXXX-XXXX-5847</div>
              </div>
              <div className="font-serif text-[18px] font-semibold text-success">₹4,82,300</div>
            </div>
            <div className="flex items-center justify-between p-3 bg-bg rounded-lg">
              <div>
                <div className="text-[13px] font-semibold">SBI — Savings</div>
                <div className="text-[11px] text-text-muted">XXXX-XXXX-2341</div>
              </div>
              <div className="font-serif text-[18px] font-semibold text-success">₹1,24,500</div>
            </div>
          </div>
        </Card>

        <Card>
          <div className="text-[14px] font-semibold mb-4">Recent Transactions</div>
          <div className="space-y-2">
            {TRANSACTIONS.map((txn, i) => (
              <div key={i} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                <div>
                  <div className="text-[13px] font-medium">{txn.desc}</div>
                  <div className="text-[11px] text-text-muted">{txn.date}</div>
                </div>
                <span className={`font-mono text-[13px] font-semibold ${txn.type === 'credit' ? 'text-success' : 'text-danger'}`}>
                  {txn.type === 'credit' ? '+' : ''}₹{Math.abs(txn.amount).toLocaleString('en-IN')}
                </span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
