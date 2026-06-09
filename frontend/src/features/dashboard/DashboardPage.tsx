export default function DashboardPage() {
  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="text-sm text-gray-500">Total Clients</div>
          <div className="text-2xl font-bold">0</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="text-sm text-gray-500">Filed</div>
          <div className="text-2xl font-bold text-green-600">0</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="text-sm text-gray-500">Pending</div>
          <div className="text-2xl font-bold text-yellow-600">0</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="text-sm text-gray-500">Pending Notices</div>
          <div className="text-2xl font-bold text-red-600">0</div>
        </div>
      </div>
      {/* Widgets */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-4 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-3">Upcoming Due Dates</h2>
          <div className="text-gray-500 text-sm">No upcoming deadlines.</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-3">Quick Actions</h2>
          <div className="space-y-2">
            <button className="w-full bg-blue-50 text-blue-700 px-4 py-2 rounded text-left hover:bg-blue-100">
              New Computation
            </button>
            <button className="w-full bg-green-50 text-green-700 px-4 py-2 rounded text-left hover:bg-green-100">
              Send Bulk OTP
            </button>
            <button className="w-full bg-purple-50 text-purple-700 px-4 py-2 rounded text-left hover:bg-purple-100">
              Add New Client
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
