import type { Category } from "../types";
export function GearArt({
  category,
  className = "",
}: {
  category: Category;
  className?: string;
}) {
  return (
    <svg
      className={`gear-art ${className}`}
      viewBox="0 0 280 190"
      fill="none"
      aria-hidden="true"
    >
      <ellipse cx="140" cy="161" rx="77" ry="9" fill="#352539" opacity=".09" />
      {category === "CAMERAS" ? (
        <g>
          <path
            d="M59 73q0-11 12-11h28l13-20h45l13 20h36q14 0 14 14v65q0 13-14 13H72q-13 0-13-13Z"
            fill="#39333C"
          />
          <path d="M59 86h161v42H59Z" fill="#4E454F" />
          <path d="M68 64h24v12H68z" fill="#817980" />
          <rect x="180" y="70" width="24" height="11" rx="3" fill="#B6B0AC" />
          <circle cx="140" cy="109" r="48" fill="#26242B" />
          <circle cx="140" cy="109" r="39" fill="#65606B" />
          <circle cx="140" cy="109" r="31" fill="#1D2635" />
          <circle cx="140" cy="109" r="22" fill="#445064" />
          <path
            d="M124 94q18-14 31 4"
            stroke="#B4C1CB"
            strokeWidth="7"
            strokeLinecap="round"
            opacity=".65"
          />
          <circle cx="146" cy="117" r="8" fill="#1F2A3C" />
          <rect x="115" y="48" width="39" height="12" rx="3" fill="#4B424C" />
          <circle cx="196" cy="92" r="3" fill="#F2D676" />
          <path d="M62 80h12v53H62z" fill="#2E2932" />
        </g>
      ) : category === "AUDIO" ? (
        <g>
          <path d="M92 67q0-34 48-34t48 34" stroke="#352E3B" strokeWidth="17" />
          <path d="M101 62q0-20 39-20t39 20" stroke="#81758C" strokeWidth="5" />
          <path d="M91 70v61m98-61v61" stroke="#7A6B80" strokeWidth="11" />
          <rect x="72" y="81" width="40" height="70" rx="17" fill="#3A3043" />
          <rect x="81" y="90" width="17" height="50" rx="8" fill="#6B5B78" />
          <rect x="168" y="81" width="40" height="70" rx="17" fill="#3A3043" />
          <rect x="180" y="90" width="17" height="50" rx="8" fill="#6B5B78" />
          <path
            d="M192 151q-3 18-32 10"
            stroke="#57485F"
            strokeWidth="4"
            strokeLinecap="round"
          />
          <rect x="147" y="155" width="23" height="8" rx="4" fill="#352E3B" />
        </g>
      ) : category === "LIGHTING" ? (
        <g>
          <path
            d="M138 93v62m0-11-37 22m37-22 38 22"
            stroke="#4A3C45"
            strokeWidth="7"
            strokeLinecap="round"
          />
          <path d="m92 39 62-16 42 41-18 60-64 15-42-41Z" fill="#4D414B" />
          <path d="m99 47 53-14 34 34-15 48-53 14-35-34Z" fill="#F2D676" />
          <path d="m105 53 44-11 29 28-13 38-44 12-28-28Z" fill="#FFF2B0" />
          <path d="m111 61 34-9 23 21-10 29-34 9-23-22Z" fill="#FFFCEB" />
          <rect x="132" y="127" width="15" height="11" rx="3" fill="#6B5260" />
        </g>
      ) : category === "COMPUTING" ? (
        <g>
          <rect x="66" y="37" width="151" height="102" rx="10" fill="#4D414D" />
          <rect x="73" y="44" width="137" height="86" rx="5" fill="#AFB9C6" />
          <path d="M73 108 210 57v73H73Z" fill="#D9C4D0" />
          <path d="m73 126 137-27v31H73Z" fill="#E7DABF" />
          <path d="M66 139h151l24 15q3 6-7 9H48q-9-3-4-9Z" fill="#7E717D" />
          <path d="M49 153h187l-2 7H49Z" fill="#B1A6B0" />
          <path d="M117 143h48l7 8h-62Z" fill="#554655" />
          <circle cx="142" cy="41" r="2" fill="#C4BAC4" />
        </g>
      ) : (
        <g>
          <path
            d="M140 45v95m0-23-49 48m49-48 49 48m-49-48v48"
            stroke="#4D424B"
            strokeWidth="8"
            strokeLinecap="round"
          />
          <rect x="111" y="31" width="58" height="29" rx="7" fill="#3B303D" />
          <rect x="123" y="23" width="34" height="17" rx="4" fill="#776471" />
          <path
            d="m166 43 27-18"
            stroke="#514051"
            strokeWidth="6"
            strokeLinecap="round"
          />
          <circle cx="140" cy="81" r="11" fill="#E4B76F" />
          <rect x="133" y="100" width="14" height="18" rx="4" fill="#857280" />
          <path
            d="m116 141-15 13m64-13 15 13"
            stroke="#897382"
            strokeWidth="4"
            strokeLinecap="round"
          />
        </g>
      )}
    </svg>
  );
}
export function GearShelf() {
  return (
    <div className="gear-shelf" aria-hidden="true">
      <span className="shelf-spark spark-one">✳</span>
      <span className="shelf-spark spark-two">✦</span>
      <div className="shelf-card shelf-camera">
        <span className="shelf-label">Camera kit</span>
        <GearArt category="CAMERAS" />
        <span className="shelf-sticker">Made to be shared</span>
      </div>
      <div className="shelf-card shelf-audio">
        <GearArt category="AUDIO" />
        <span>Sound good.</span>
      </div>
      <div className="shelf-card shelf-laptop">
        <GearArt category="COMPUTING" />
      </div>
      <div className="shelf-floor" />
    </div>
  );
}
