const fs = require('fs');
const path = require('path');

// Files to update
const files = [
  'frontend/src/components/itr2/ITR2HouseProperty.tsx',
  'frontend/src/components/itr2/ITR2CapitalGains.tsx',
  'frontend/src/components/itr2/ITR2OtherSources.tsx',
  'frontend/src/components/itr2/ITR2Deductions.tsx',
  'frontend/src/components/itr2/ITR2Losses.tsx',
  'frontend/src/components/itr3/ITR3Salary.tsx',
  'frontend/src/components/itr3/ITR3Business.tsx',
  'frontend/src/components/itr3/ITR3ProfitLoss.tsx',
  'frontend/src/components/itr3/ITR3BalanceSheet.tsx',
  'frontend/src/components/itr3/ITR3Depreciation.tsx',
  'frontend/src/components/itr3/ITR3HouseProperty.tsx',
  'frontend/src/components/itr3/ITR3CapitalGains.tsx',
  'frontend/src/components/itr3/ITR3OtherSources.tsx',
  'frontend/src/components/itr3/ITR3Deductions.tsx',
  'frontend/src/components/itr3/ITR3Losses.tsx',
  'frontend/src/components/itr4/ITR4Salary.tsx',
  'frontend/src/components/itr4/ITR4Presumptive.tsx',
  'frontend/src/components/itr4/ITR4HouseProperty.tsx',
  'frontend/src/components/itr4/ITR4OtherSources.tsx',
  'frontend/src/components/itr4/ITR4Deductions.tsx',
  'frontend/src/components/itr4/ITR4TaxesPaid.tsx'
];

files.forEach(file => {
  const filePath = path.join(__dirname, file);
  let content = fs.readFileSync(filePath, 'utf8');
  
  // Add import if not present
  if (!content.includes('formatIndianNumber')) {
    content = content.replace(
      /^(import.*from.*['"]@\/lib\/tax-calculator['"];?)$/m,
      "$1\nimport { formatIndianNumber, parseIndianNumber } from '@/lib/input-utils';"
    );
  }
  
  // Replace type="number" with type="text" and update value/onChange
  content = content.replace(
    /type="number"\s+style=\{([^}]+)\}\s+value=\{([^}|]+)\s*\|\|\s*['"]['"]?\}\s+onChange=\{([^}]+)Number\(e\.target\.value\)([^}]*)\}/g,
    (match, style, field, onChangeStart, onChangeEnd) => {
      return `type="text" style={${style}} value={formatIndianNumber(${field.trim()})} onChange={${onChangeStart}parseIndianNumber(e.target.value)${onChangeEnd}}`;
    }
  );
  
  fs.writeFileSync(filePath, content, 'utf8');
  console.log(`Updated: ${file}`);
});

console.log('All files updated with Indian formatting!');
